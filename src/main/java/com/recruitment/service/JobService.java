package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Job;
import com.recruitment.model.Notification;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for managing Job postings.
 * Handles CRUD operations, filtering, searching, sorting,
 * statistics, and automatic expiration checks.
 *
 * Data is persisted in {@code data/jobs.json} via {@link JsonUtil}.
 */
public class JobService {
    private static final String FILE_NAME = "jobs.json";
    private static final Type LIST_TYPE = new TypeToken<List<Job>>() {}.getType();

    /** In-memory cache of all job postings, loaded from JSON on construction. */
    private List<Job> jobs;

    /**
     * Injected via setter to avoid circular dependency.
     * Used by {@link #checkExpiredJobs()} to send expiration notifications.
     */
    private NotificationService notificationService;

    /**
     * Injected via setter to avoid circular dependency.
     * Used by {@link #checkExpiredJobs()} to find pending applications for expired jobs.
     */
    private ApplicationService applicationService;

    public JobService() {
        this.jobs = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Reloads job data from the JSON file, discarding in-memory changes. */
    public void reload() {
        this.jobs = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    /** Persists the current in-memory job list back to JSON. */
    private void save() {
        JsonUtil.saveList(FILE_NAME, jobs);
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new job posting.
     * Automatically assigns a unique ID, sets the post date to today,
     * and marks the status as OPEN.
     */
    public Job createJob(Job job) {
        job.setId(IDGenerator.generate("JOB"));
        job.setPostDate(LocalDate.now().toString());
        job.setStatus(Job.Status.OPEN);
        jobs.add(job);
        save();
        return job;
    }

    /**
     * Updates an existing job by matching on {@code job.getId()}.
     * @return true if the job was found and updated, false otherwise.
     */
    public boolean updateJob(Job job) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId().equals(job.getId())) {
                jobs.set(i, job);
                save();
                return true;
            }
        }
        return false;
    }

    /** Finds a job by its unique ID. */
    public Optional<Job> findById(String id) {
        return jobs.stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    /** Returns all jobs with status OPEN. */
    public List<Job> getOpenJobs() {
        return jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).collect(Collectors.toList());
    }

    /** Returns all jobs posted by a specific Module Organiser (by user ID). */
    public List<Job> getJobsByMO(String moId) {
        return jobs.stream().filter(j -> j.getPostedBy().equals(moId)).collect(Collectors.toList());
    }

    /** Returns the full list of all jobs regardless of status. */
    public List<Job> getAllJobs() {
        return jobs;
    }

    /**
     * Permanently deletes a job by ID.
     * Note: associated applications are NOT cleaned up — see known issues.
     * @return true if a job was removed, false if the ID was not found.
     */
    public boolean deleteJob(String id) {
        boolean removed = jobs.removeIf(j -> j.getId().equals(id));
        if (removed) save();
        return removed;
    }

    /**
     * Closes a job so it no longer accepts applications.
     * Sets the status to CLOSED.
     * @return true if successful, false if the job ID was not found.
     */
    public boolean closeJob(String id) {
        Optional<Job> job = findById(id);
        if (job.isPresent()) {
            job.get().setStatus(Job.Status.CLOSED);
            save();
            return true;
        }
        return false;
    }

    // ==================== Filtering ====================

    /**
     * Filters jobs by status and/or job type.
     * Pass {@code null} for either parameter to skip that filter.
     */
    public List<Job> filterJobs(Job.Status status, Job.JobType jobType) {
        return jobs.stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> jobType == null || j.getJobType() == jobType)
                .collect(Collectors.toList());
    }

    /** Returns jobs whose deadline matches the given date string exactly. */
    public List<Job> filterJobsByDeadline(String deadline) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null && j.getDeadline().equals(deadline))
                .collect(Collectors.toList());
    }

    /** Returns jobs belonging to a specific module. */
    public List<Job> filterJobsByModule(String moduleName) {
        return jobs.stream()
                .filter(j -> j.getModuleName() != null && j.getModuleName().equals(moduleName))
                .collect(Collectors.toList());
    }

    // ==================== Searching ====================

    /**
     * Searches jobs by keyword across title, description, and module name.
     * Case-insensitive partial match.
     */
    public List<Job> searchJobs(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getTitle().toLowerCase().contains(lowerKeyword) ||
                        (j.getDescription() != null && j.getDescription().toLowerCase().contains(lowerKeyword)) ||
                        (j.getModuleName() != null && j.getModuleName().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    /** Searches jobs whose required skills contain the given skill (case-insensitive). */
    public List<Job> searchJobsBySkill(String skill) {
        String lowerSkill = skill.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getRequiredSkills().stream()
                        .anyMatch(s -> s.toLowerCase().contains(lowerSkill)))
                .collect(Collectors.toList());
    }

    // ==================== Sorting ====================

    /** Sorts all jobs alphabetically by title. */
    public List<Job> sortJobsByTitle(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getTitle) :
                        Comparator.comparing(Job::getTitle).reversed())
                .collect(Collectors.toList());
    }

    /** Sorts all jobs by post date (string comparison, assumes yyyy-MM-dd format). */
    public List<Job> sortJobsByPostDate(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getPostDate) :
                        Comparator.comparing(Job::getPostDate).reversed())
                .collect(Collectors.toList());
    }

    /** Sorts jobs by deadline. Jobs without a deadline are excluded. */
    public List<Job> sortJobsByDeadline(boolean ascending) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null)
                .sorted(ascending ?
                        Comparator.comparing(Job::getDeadline) :
                        Comparator.comparing(Job::getDeadline).reversed())
                .collect(Collectors.toList());
    }

    /** Sorts jobs by the number of remaining available positions. */
    public List<Job> sortJobsByAvailablePositions(boolean ascending) {
        Comparator<Job> comparator = (j1, j2) -> Integer.compare(
                j1.getMaxPositions() - j1.getFilledPositions(),
                j2.getMaxPositions() - j2.getFilledPositions());
        return jobs.stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .collect(Collectors.toList());
    }

    // ==================== Statistics ====================

    /** Returns a summary map with counts of total, open, closed, and filled jobs. */
    public Map<String, Integer> getJobStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalJobs", jobs.size());
        stats.put("openJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).count());
        stats.put("closedJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.CLOSED).count());
        stats.put("filledJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.FILLED).count());
        return stats;
    }

    /** Returns a breakdown of job counts grouped by {@link Job.JobType}. */
    public Map<Job.JobType, Integer> getJobCountByType() {
        Map<Job.JobType, Integer> counts = new HashMap<>();
        for (Job.JobType type : Job.JobType.values()) {
            counts.put(type, (int) jobs.stream().filter(j -> j.getJobType() == type).count());
        }
        return counts;
    }

    /** Sums up all remaining unfilled positions across every job. */
    public int getTotalAvailablePositions() {
        return jobs.stream()
                .mapToInt(j -> j.getMaxPositions() - j.getFilledPositions())
                .sum();
    }

    /** Sums up all filled positions across every job. */
    public int getTotalFilledPositions() {
        return jobs.stream()
                .mapToInt(Job::getFilledPositions)
                .sum();
    }

    /** Returns a map of MO user IDs to the number of jobs each has posted. */
    public Map<String, Integer> getJobCountByMO() {
        Map<String, Integer> counts = new HashMap<>();
        jobs.forEach(j -> counts.put(j.getPostedBy(), counts.getOrDefault(j.getPostedBy(), 0) + 1));
        return counts;
    }

    // ==================== Expiration Check ====================

    /**
     * Scans all OPEN jobs and closes any whose deadline has passed.
     * For each expired job, sends a POSITION_EXPIRATION notification
     * to every applicant who still has a PENDING application.
     *
     * Called periodically by a background Timer in {@link com.recruitment.Main}.
     * Requires both {@code applicationService} and {@code notificationService}
     * to be injected before invocation.
     */
    public void checkExpiredJobs() {
        LocalDate today = LocalDate.now();
        for (Job job : jobs) {
            if (job.getStatus() == Job.Status.OPEN && job.getDeadline() != null) {
                LocalDate deadline = LocalDate.parse(job.getDeadline());
                if (deadline.isBefore(today) || deadline.isEqual(today)) {
                    job.setStatus(Job.Status.CLOSED);
                    save();

                    List<com.recruitment.model.Application> applications = applicationService.getApplicationsByJob(job.getId());
                    for (com.recruitment.model.Application app : applications) {
                        if (app.getStatus() == com.recruitment.model.Application.Status.PENDING) {
                            notificationService.createNotification(
                                app.getApplicantId(),
                                "The position '" + job.getTitle() + "' has expired.",
                                Notification.Type.POSITION_EXPIRATION
                            );
                        }
                    }
                }
            }
        }
    }
}
