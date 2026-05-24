package com.recruitment.service;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.User;
import com.recruitment.util.AppConfig;
import com.recruitment.util.DeepSeekClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stateless analysis service for skill matching and TA workload balancing.
 * <p>
 * Performs rule-based scoring over in-memory {@link User}, {@link Job}, and
 * {@link Application} data supplied by callers. Optionally enriches top results with
 * narrative comments from the DeepSeek API when enabled in {@link AppConfig}.
 * </p>
 * <p>
 * This service does not persist data; it reads from other services and returns
 * analysis DTOs ({@link CandidateAnalysis}, {@link WorkloadAnalysisResult}, etc.).
 * </p>
 */
public class AIAnalysisService {

    /**
     * Relative priority assigned to a job-required skill that a TA lacks,
     * based on position in the required-skills list.
     */
    public enum SkillImportance {
        /** Listed in the first third of required skills. */
        HIGH("High"),
        /** Listed in the middle third of required skills. */
        MEDIUM("Medium"),
        /** Listed in the last third of required skills. */
        LOW("Low");

        private final String label;

        SkillImportance(String label) { this.label = label; }

        /**
         * @return display label for UI and reports (e.g. {@code "High"})
         */
        public String getLabel() { return label; }
    }

    /**
     * Skill match outcome for a single TA against a single job (legacy/simple report shape).
     */
    public static class SkillMatchResult {
        /** The teaching assistant being evaluated. */
        public final User ta;
        /** The job posting being matched against. */
        public final Job job;
        /** Percentage of required skills present on the TA profile (0–100). */
        public final double matchPercent;
        /** Required skills the TA possesses (case-insensitive match). */
        public final List<String> matchedSkills;
        /** Required skills the TA lacks, each tagged with {@link SkillImportance}. */
        public final List<MissingSkill> missingSkills;

        /**
         * @param ta            the TA user
         * @param job           the target job
         * @param matchPercent  match percentage
         * @param matchedSkills skills satisfied
         * @param missingSkills skills missing with importance
         */
        public SkillMatchResult(User ta, Job job, double matchPercent,
                                List<String> matchedSkills, List<MissingSkill> missingSkills) {
            this.ta = ta;
            this.job = job;
            this.matchPercent = matchPercent;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }
    }

    /**
     * A required skill that the TA does not have, with an assigned importance level.
     */
    public static class MissingSkill {
        /** The skill name as listed on the job. */
        public final String skill;
        /** Importance derived from the skill's index in the required list. */
        public final SkillImportance importance;

        /**
         * @param skill      the missing skill name
         * @param importance how critical the skill is considered for this job
         */
        public MissingSkill(String skill, SkillImportance importance) {
            this.skill = skill;
            this.importance = importance;
        }
    }

    /**
     * Per-applicant skill analysis for a job, including optional DeepSeek commentary.
     */
    public static class CandidateAnalysis {
        /** The applicant TA profile. */
        public final User ta;
        /** The pending or reviewed application. */
        public final Application application;
        /** Percentage of required skills matched (0–100; 100 if job has no required skills). */
        public final double matchPercent;
        /** Required skills the TA possesses. */
        public final List<String> matchedSkills;
        /** Required skills the TA lacks with importance tags. */
        public final List<MissingSkill> missingSkills;
        /** DeepSeek-generated narrative analysis; {@code null} if API disabled or call failed. */
        public String aiComment;

        /**
         * @param ta             the applicant
         * @param application    their application for the job
         * @param matchPercent   skill match percentage
         * @param matchedSkills  satisfied skills
         * @param missingSkills  gaps with importance
         */
        public CandidateAnalysis(User ta, Application application, double matchPercent,
                                 List<String> matchedSkills, List<MissingSkill> missingSkills) {
            this.ta = ta;
            this.application = application;
            this.matchPercent = matchPercent;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }
    }

    /**
     * Aggregate workload analysis across multiple TAs, with balancing suggestions.
     */
    public static class WorkloadAnalysisResult {
        /** Per-TA workload metrics and status labels. */
        public final List<TAWorkload> workloads;
        /** Rule-based reassignment suggestions (may be empty). */
        public final List<WorkloadSuggestion> suggestions;
        /** Mean workload score across all TAs in the analysis. */
        public final double avgWorkload;
        /** Human-readable summary of distribution and suggestion count. */
        public final String summary;
        /** DeepSeek-generated workload balance narrative; {@code null} if API disabled or failed. */
        public String aiComment;

        /**
         * @param workloads   classified TA workloads
         * @param suggestions generated balancing actions
         * @param avgWorkload average workload score
         * @param summary     text summary for display
         */
        public WorkloadAnalysisResult(List<TAWorkload> workloads,
                                      List<WorkloadSuggestion> suggestions,
                                      double avgWorkload, String summary) {
            this.workloads = workloads;
            this.suggestions = suggestions;
            this.avgWorkload = avgWorkload;
            this.summary = summary;
        }
    }

    /**
     * Workload metrics and classification for one TA.
     */
    public static class TAWorkload {
        /** The TA user. */
        public final User ta;
        /** Count of {@link Application.Status#ACCEPTED} applications. */
        public final int acceptedJobs;
        /** Count of {@link Application.Status#PENDING} applications. */
        public final int pendingApps;
        /** Score: accepted × 3 + pending × 1. */
        public final double workloadScore;
        /** One of {@code "Overloaded"}, {@code "Balanced"}, or {@code "Available"}. */
        public final String status;

        /**
         * @param ta            the TA
         * @param acceptedJobs  accepted application count
         * @param pendingApps   pending application count
         * @param workloadScore computed score
         * @param status        workload classification label
         */
        public TAWorkload(User ta, int acceptedJobs, int pendingApps, double workloadScore, String status) {
            this.ta = ta;
            this.acceptedJobs = acceptedJobs;
            this.pendingApps = pendingApps;
            this.workloadScore = workloadScore;
            this.status = status;
        }
    }

    /**
     * A suggested workload rebalance (typically reassigning a pending application).
     */
    public static class WorkloadSuggestion {
        /** Suggestion category, e.g. {@code "REASSIGN"} or {@code "BALANCE"}. */
        public final String type;
        /** Full description for MO review. */
        public final String description;
        /** Name of the overloaded TA (source). */
        public final String fromTA;
        /** Name of the underloaded TA (target). */
        public final String toTA;
        /** Application ID involved in the suggestion. */
        public final String applicationId;
        /** Job title for context. */
        public final String jobTitle;
        /** Skill match percentage between target TA and job (0–100). */
        public final double matchScore;
        /** Whether the MO has marked this suggestion as adopted in the UI. */
        public boolean adopted;

        /**
         * @param type           suggestion type code
         * @param description    human-readable explanation
         * @param fromTA         overloaded TA display name
         * @param toTA           available TA display name
         * @param applicationId  related application ID
         * @param jobTitle       job title
         * @param matchScore     target TA skill match to the job
         */
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

    private DeepSeekClient deepSeek() {
        return new DeepSeekClient();
    }

    /**
     * Analyses skill match between applicants and a job's required skills.
     * <p>
     * Results are sorted by match percentage descending. When DeepSeek is enabled,
     * the top three candidates receive an {@link CandidateAnalysis#aiComment}.
     * </p>
     *
     * @param job         the job posting (required skills taken from {@link Job#getRequiredSkills()})
     * @param applications applications to evaluate (typically for this job)
     * @param userService used to resolve applicant {@link User} profiles
     * @return sorted list of {@link CandidateAnalysis}; skips applications whose applicant is unknown
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
     * First third → HIGH, middle third → MEDIUM, last third → LOW.
     *
     * @param index zero-based index of the skill in the required list
     * @param total number of required skills
     * @return the assigned {@link SkillImportance}
     */
    private SkillImportance assignImportance(int index, int total) {
        if (total <= 1) return SkillImportance.HIGH;
        double ratio = (double) index / (total - 1);
        if (ratio < 0.34) return SkillImportance.HIGH;
        if (ratio < 0.67) return SkillImportance.MEDIUM;
        return SkillImportance.LOW;
    }

    /**
     * Formats candidate analysis results as a plain-text report for export or display.
     *
     * @param job     the analysed job (title, module, skills included in header)
     * @param results output from {@link #analyzeJobApplicants(Job, List, UserService)}
     * @return multi-line report string including optional DeepSeek sections
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
     * Analyses TA workload distribution and generates rebalancing suggestions.
     * <p>
     * Workload score is {@code accepted × 3 + pending × 1}. TAs are classified as
     * Overloaded, Balanced, or Available relative to the average ± 30%. When DeepSeek
     * is enabled, {@link WorkloadAnalysisResult#aiComment} is populated.
     * </p>
     *
     * @param tas        list of TA users to analyse
     * @param appService source of application counts per TA
     * @param jobService used to resolve job details for suggestions
     * @return aggregate {@link WorkloadAnalysisResult} with metrics, suggestions, and summary
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
