package com.recruitment.service;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.util.AppConfig;
import com.recruitment.util.DeepSeekClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-assisted analysis service.
 * Provides skill matching analysis and workload balancing recommendations.
 */
public class AIAnalysisService {

    public enum SkillImportance {
        HIGH("High"), MEDIUM("Medium"), LOW("Low");
        private final String label;
        SkillImportance(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static class SkillMatchResult {
        public final User ta;
        public final Job job;
        public final double matchPercent;
        public final List<String> matchedSkills;
        public final List<MissingSkill> missingSkills;

        public SkillMatchResult(User ta, Job job, double matchPercent,
                                List<String> matchedSkills, List<MissingSkill> missingSkills) {
            this.ta = ta;
            this.job = job;
            this.matchPercent = matchPercent;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }
    }

    public static class MissingSkill {
        public final String skill;
        public final SkillImportance importance;

        public MissingSkill(String skill, SkillImportance importance) {
            this.skill = skill;
            this.importance = importance;
        }
    }

    public static class CandidateAnalysis {
        public final User ta;
        public final Application application;
        public final double matchPercent;
        public final List<String> matchedSkills;
        public final List<MissingSkill> missingSkills;
        /** DeepSeek-generated narrative analysis (null if API disabled/failed) */
        public String aiComment;

        public CandidateAnalysis(User ta, Application application, double matchPercent,
                                 List<String> matchedSkills, List<MissingSkill> missingSkills) {
            this.ta = ta;
            this.application = application;
            this.matchPercent = matchPercent;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }
    }

    public static class WorkloadAnalysisResult {
        public final List<TAWorkload> workloads;
        public final List<WorkloadSuggestion> suggestions;
        public final double avgWorkload;
        public final String summary;
        /** DeepSeek-generated workload balance narrative (null if API disabled/failed) */
        public String aiComment;

        public WorkloadAnalysisResult(List<TAWorkload> workloads,
                                      List<WorkloadSuggestion> suggestions,
                                      double avgWorkload, String summary) {
            this.workloads = workloads;
            this.suggestions = suggestions;
            this.avgWorkload = avgWorkload;
            this.summary = summary;
        }
    }

    public static class TAWorkload {
        public final User ta;
        public final int acceptedJobs;
        public final int pendingApps;
        public final double workloadScore;
        public final String status; // OVERLOADED / BALANCED / UNDERLOADED

        public TAWorkload(User ta, int acceptedJobs, int pendingApps, double workloadScore, String status) {
            this.ta = ta;
            this.acceptedJobs = acceptedJobs;
            this.pendingApps = pendingApps;
            this.workloadScore = workloadScore;
            this.status = status;
        }
    }

    public static class WorkloadSuggestion {
        public final String type;           // "REASSIGN" / "BALANCE"
        public final String description;
        public final String fromTA;
        public final String toTA;
        public final String applicationId;
        public final String jobTitle;
        public final double matchScore;     // match between toTA and the job
        public boolean adopted;

        public WorkloadSuggestion(String type, String description,
                                   String fromTA, String toTA,
                                   String applicationId, String jobTitle,
                                   double matchScore) {
            this.type = type;
            this.description = description;
            this.fromTA = fromTA;
            this.toTA = toTA;
            this.applicationId = applicationId;
            this.jobTitle = jobTitle;
            this.matchScore = matchScore;
            this.adopted = false;
        }
    }

    // ─── DeepSeek client (lazy-initialised) ────────────────────────────────
    private DeepSeekClient deepSeek() {
        return new DeepSeekClient();
    }

    /**
     * Analyse skill match between a list of applicants and a job.
     * Results are sorted by match percentage (descending).
     * If DeepSeek is enabled, the top-3 candidates also receive an AI narrative comment.
     */
    public List<CandidateAnalysis> analyzeJobApplicants(Job job,
                                                         List<Application> applications,
                                                         UserService userService) {
        List<CandidateAnalysis> results = new ArrayList<>();
        List<String> required = job.getRequiredSkills();
        if (required == null) required = Collections.emptyList();

        for (Application app : applications) {
            Optional<User> taOpt = userService.findById(app.getApplicantId());
            if (!taOpt.isPresent()) continue;
            User ta = taOpt.get();

            List<String> taSkills = ta.getSkills() != null ? ta.getSkills() : Collections.emptyList();
            List<String> taSkillsLower = taSkills.stream()
                    .map(String::toLowerCase).collect(Collectors.toList());

            List<String> matched = new ArrayList<>();
            List<MissingSkill> missing = new ArrayList<>();

            for (int i = 0; i < required.size(); i++) {
                String skill = required.get(i);
                if (taSkillsLower.contains(skill.toLowerCase())) {
                    matched.add(skill);
                } else {
                    SkillImportance importance = assignImportance(i, required.size());
                    missing.add(new MissingSkill(skill, importance));
                }
            }

            double percent = required.isEmpty() ? 100.0
                    : (double) matched.size() / required.size() * 100.0;
            results.add(new CandidateAnalysis(ta, app, percent, matched, missing));
        }

        // Sort by match percentage descending
        results.sort((a, b) -> Double.compare(b.matchPercent, a.matchPercent));

        // Enrich top-3 results with DeepSeek narrative (async feel – runs in same thread)
        if (AppConfig.isDeepSeekEnabled()) {
            DeepSeekClient ds = deepSeek();
            String jobRequired = required.isEmpty() ? "None" : String.join(", ", required);
            int limit = Math.min(3, results.size());
            for (int i = 0; i < limit; i++) {
                CandidateAnalysis r = results.get(i);
                String missing = r.missingSkills.stream()
                        .map(m -> m.skill + "(" + m.importance.getLabel() + ")")
                        .collect(Collectors.joining(", "));
                String taSkills = r.ta.getSkills() != null
                        ? String.join(", ", r.ta.getSkills()) : "None";
                String comment = ds.analyzeSkillGap(
                        job.getTitle(), jobRequired,
                        r.ta.getName(), taSkills,
                        r.matchPercent, missing.isEmpty() ? "None" : missing);
                r.aiComment = comment; // may be null if API fails – caller handles gracefully
            }
        }

        return results;
    }

    /**
     * Assigns importance to a missing skill based on its position in the required skills list.
     * First 1/3 → HIGH, middle 1/3 → MEDIUM, last 1/3 → LOW.
     */
    private SkillImportance assignImportance(int index, int total) {
        if (total <= 1) return SkillImportance.HIGH;
        double ratio = (double) index / (total - 1);
        if (ratio < 0.34) return SkillImportance.HIGH;
        if (ratio < 0.67) return SkillImportance.MEDIUM;
        return SkillImportance.LOW;
    }

    /**
     * Export candidate analysis results to a formatted text string.
     */
    public String exportAnalysisToText(Job job, List<CandidateAnalysis> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("AI Skill Analysis Report\n");
        sb.append("========================================\n");
        sb.append("Job: ").append(job.getTitle()).append("\n");
        sb.append("Module: ").append(job.getModuleName() != null ? job.getModuleName() : "N/A").append("\n");
        sb.append("Required Skills: ").append(
                job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "None"
        ).append("\n");
        sb.append("Generated: ").append(java.time.LocalDateTime.now().toString().replace("T", " ")).append("\n");
        sb.append("----------------------------------------\n\n");
        sb.append(String.format("%-4s %-20s %-10s %-30s %s\n",
                "Rank", "Applicant", "Match%", "Matched Skills", "Missing Skills (Importance)"));
        sb.append("----------------------------------------\n");

        int rank = 1;
        for (CandidateAnalysis r : results) {
            String matchedStr = r.matchedSkills.isEmpty() ? "None" : String.join(", ", r.matchedSkills);
            String missingStr = r.missingSkills.isEmpty() ? "None" :
                    r.missingSkills.stream()
                            .map(m -> m.skill + "[" + m.importance.getLabel() + "]")
                            .collect(Collectors.joining(", "));
            sb.append(String.format("%-4d %-20s %-10s %-30s %s\n",
                    rank++,
                    truncate(r.ta.getName(), 18),
                    String.format("%.1f%%", r.matchPercent),
                    truncate(matchedStr, 28),
                    missingStr));
        }
        sb.append("\n========================================\n");
        sb.append("Total candidates: ").append(results.size()).append("\n");

        // Append DeepSeek AI comments for candidates that have them
        boolean hasAiComments = results.stream().anyMatch(r -> r.aiComment != null);
        if (hasAiComments) {
            sb.append("\n========================================\n");
            sb.append("DeepSeek AI In-depth Analysis (Top Candidates)\n");
            sb.append("========================================\n");
            int r = 1;
            for (CandidateAnalysis ca : results) {
                if (ca.aiComment != null) {
                    sb.append("\n[Rank ").append(r).append("] ").append(ca.ta.getName())
                      .append(" (").append(String.format("%.1f%%", ca.matchPercent)).append(")\n");
                    sb.append(ca.aiComment).append("\n");
                }
                r++;
            }
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    /**
     * Analyse TA workload and produce balancing suggestions.
     */
    public WorkloadAnalysisResult analyzeWorkload(List<User> tas,
                                                   ApplicationService appService,
                                                   JobService jobService) {
        List<TAWorkload> workloads = new ArrayList<>();
        double totalScore = 0;

        for (User ta : tas) {
            List<Application> apps = appService.getApplicationsByApplicant(ta.getId());
            int accepted = (int) apps.stream()
                    .filter(a -> a.getStatus() == Application.Status.ACCEPTED).count();
            int pending = (int) apps.stream()
                    .filter(a -> a.getStatus() == Application.Status.PENDING).count();
            // Workload score: each accepted job = 3 pts, each pending = 1 pt
            double score = accepted * 3.0 + pending * 1.0;
            totalScore += score;
            workloads.add(new TAWorkload(ta, accepted, pending, score, ""));
        }

        double avg = tas.isEmpty() ? 0 : totalScore / tas.size();
        double threshold = avg * 0.3; // 30% tolerance

        // Classify workload status
        List<TAWorkload> classified = new ArrayList<>();
        for (TAWorkload w : workloads) {
            String status;
            if (w.workloadScore > avg + threshold) status = "Overloaded";
            else if (w.workloadScore < avg - threshold && avg > 0) status = "Available";
            else status = "Balanced";
            classified.add(new TAWorkload(w.ta, w.acceptedJobs, w.pendingApps, w.workloadScore, status));
        }

        // Generate suggestions
        List<WorkloadSuggestion> suggestions = generateSuggestions(classified, avg, appService, jobService);

        String summary = String.format(
                "Total TAs: %d  |  Average Workload Score: %.1f\nOverloaded: %d  |  Balanced: %d  |  Available: %d\n%s",
                tas.size(), avg,
                classified.stream().filter(w -> w.status.equals("Overloaded")).count(),
                classified.stream().filter(w -> w.status.equals("Balanced")).count(),
                classified.stream().filter(w -> w.status.equals("Available")).count(),
                suggestions.isEmpty() ? "Workload distribution looks balanced. No adjustments needed." :
                        suggestions.size() + " workload rebalancing suggestion(s) generated. Please review below."
        );

        WorkloadAnalysisResult result = new WorkloadAnalysisResult(classified, suggestions, avg, summary);

        // Enrich with DeepSeek narrative analysis
        if (AppConfig.isDeepSeekEnabled()) {
            StringBuilder workloadData = new StringBuilder();
            workloadData.append(String.format("Average workload score: %.1f\n\n", avg));
            for (TAWorkload w : classified) {
                workloadData.append(String.format(
                        "- %s: accepted=%d, pending=%d, score=%.0f, status=%s, skills=[%s]\n",
                        w.ta.getName(), w.acceptedJobs, w.pendingApps, w.workloadScore, w.status,
                        w.ta.getSkills() != null ? String.join(", ", w.ta.getSkills()) : "N/A"));
            }
            if (!suggestions.isEmpty()) {
                workloadData.append("\nRule-based suggestions:\n");
                for (WorkloadSuggestion s : suggestions) {
                    workloadData.append("- ").append(s.description).append("\n");
                }
            }
            result.aiComment = deepSeek().analyzeWorkloadBalance(workloadData.toString());
        }

        return result;
    }

    private List<WorkloadSuggestion> generateSuggestions(List<TAWorkload> workloads,
                                                           double avg,
                                                           ApplicationService appService,
                                                           JobService jobService) {
        List<WorkloadSuggestion> suggestions = new ArrayList<>();

        List<TAWorkload> overloaded = workloads.stream()
                .filter(w -> w.status.equals("Overloaded"))
                .sorted((a, b) -> Double.compare(b.workloadScore, a.workloadScore))
                .collect(Collectors.toList());

        List<TAWorkload> underloaded = workloads.stream()
                .filter(w -> w.status.equals("Available"))
                .sorted(Comparator.comparingDouble(w -> w.workloadScore))
                .collect(Collectors.toList());

        if (overloaded.isEmpty() || underloaded.isEmpty()) return suggestions;

        for (TAWorkload heavy : overloaded) {
            // Find pending applications of this overloaded TA
            List<Application> pending = appService.getApplicationsByApplicant(heavy.ta.getId())
                    .stream()
                    .filter(a -> a.getStatus() == Application.Status.PENDING)
                    .collect(Collectors.toList());

            for (Application app : pending) {
                Optional<com.recruitment.model.Job> jobOpt = jobService.findById(app.getJobId());
                if (!jobOpt.isPresent()) continue;
                com.recruitment.model.Job job = jobOpt.get();

                // Find best underloaded TA who hasn't applied and has skill match
                for (TAWorkload light : underloaded) {
                    // Check not already applied
                    boolean alreadyApplied = appService.getApplicationsByApplicant(light.ta.getId())
                            .stream().anyMatch(a -> a.getJobId().equals(job.getId())
                                    && a.getStatus() != Application.Status.WITHDRAWN);
                    if (alreadyApplied) continue;

                    // Calculate skill match
                    List<String> required = job.getRequiredSkills() != null ? job.getRequiredSkills() : Collections.emptyList();
                    List<String> taSkills = light.ta.getSkills() != null ? light.ta.getSkills() : Collections.emptyList();
                    long matched = required.stream()
                            .filter(s -> taSkills.stream().anyMatch(ts -> ts.equalsIgnoreCase(s)))
                            .count();
                    double matchPct = required.isEmpty() ? 100.0 : (double) matched / required.size() * 100.0;

                    if (matchPct >= 40.0) { // Only suggest if at least 40% match
                        String desc = String.format(
                                "Suggest reassigning \"%s\" from %s (score %.0f, overloaded) to %s (score %.0f, available) — skill match %.0f%%",
                                job.getTitle(), heavy.ta.getName(), heavy.workloadScore,
                                light.ta.getName(), light.workloadScore, matchPct);
                        suggestions.add(new WorkloadSuggestion(
                                "REASSIGN", desc,
                                heavy.ta.getName(), light.ta.getName(),
                                app.getId(), job.getTitle(), matchPct));
                        break; // One suggestion per overloaded TA app
                    }
                }
                if (suggestions.size() >= 10) break; // Limit suggestions
            }
            if (suggestions.size() >= 10) break;
        }
        return suggestions;
    }
}
