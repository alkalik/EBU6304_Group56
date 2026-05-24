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
 * Service layer for managing job postings.
 * <p>
 * Handles CRUD operations, filtering, searching, sorting, statistics, and automatic
 * expiration checks. Coordinates with {@link ApplicationService} and
 * {@link NotificationService} when closing expired jobs.
 * </p>
 * <p>
 * Data is persisted in {@code data/jobs.json} via {@link JsonUtil}. An in-memory list
 * is loaded at construction and written back on each mutating operation.
 * </p>
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

    /**
     * Loads all jobs from JSON into memory.
     */
    public JobService() {
        this.jobs = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    /**
     * Injects the application service (required for {@link #checkExpiredJobs()}).
     *
     * @param applicationService the {@link ApplicationService} instance to use
     */
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Injects the notification service (required for {@link #checkExpiredJobs()}).
     *
     * @param notificationService the {@link NotificationService} instance to use
     */
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Reloads job data from the JSON file, discarding unsaved in-memory changes.
     */
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
     * <p>
     * Automatically assigns a unique ID, sets the post date to today, and marks the status as OPEN.
     * </p>
     *
     * @param job the job details (ID and post date are overwritten)
     * @return the persisted job with generated ID and status {@link Job.Status#OPEN}
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
     * Updates an existing job by matching on {@link Job#getId()}.
     *
     * @param job the job record with updated fields
     * @return {@code true} if the job was found and updated; {@code false} otherwise
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

    /**
     * Finds a job by its unique ID.
     *
     * @param id the job ID (e.g. {@code JOB-...})
     * @return an {@link Optional} containing the job if found, or empty otherwise
     */
    public Optional<Job> findById(String id) {
        return jobs.stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    /**
     * Returns all jobs with status {@link Job.Status#OPEN}.
     *
     * @return a list of open jobs (may be empty)
     */
    public List<Job> getOpenJobs() {
        return jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).collect(Collectors.toList());
    }

    /**
     * Returns all jobs posted by a specific module organiser.
     *
     * @param moId the MO's user ID ({@link Job#getPostedBy()})
     * @return a list of jobs posted by that user (may be empty)
     */
    public List<Job> getJobsByMO(String moId) {
        return jobs.stream().filter(j -> j.getPostedBy().equals(moId)).collect(Collectors.toList());
    }

    /**
     * Returns the live in-memory list of all jobs regardless of status.
     *
     * @return the internal list of all jobs (never {@code null})
     */
    public List<Job> getAllJobs() {
        return jobs;
    }

    /**
     * Permanently deletes a job by ID.
     * <p>
     * Note: associated applications are not removed automatically.
     * </p>
     *
     * @param id the job ID to delete
     * @return {@code true} if a job was removed; {@code false} if the ID was not found
     */
    public boolean deleteJob(String id) {
        boolean removed = jobs.removeIf(j -> j.getId().equals(id));
        if (removed) save();
        return removed;
    }

    /**
     * Closes a job so it no longer accepts applications.
     * <p>
     * Sets the status to {@link Job.Status#CLOSED}.
     * </p>
     *
     * @param id the job ID to close
     * @return {@code true} if successful; {@code false} if the job ID was not found
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
     * <p>
     * Pass {@code null} for either parameter to skip that filter.
     * </p>
     *
     * @param status  optional {@link Job.Status} filter; {@code null} means any status
     * @param jobType optional {@link Job.JobType} filter; {@code null} means any type
     * @return a filtered list of jobs (may be empty)
     */
    public List<Job> filterJobs(Job.Status status, Job.JobType jobType) {
        return jobs.stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> jobType == null || j.getJobType() == jobType)
                .collect(Collectors.toList());
    }

    /**
     * Returns jobs whose deadline matches the given date string exactly.
     *
     * @param deadline the deadline string (expected format {@code yyyy-MM-dd})
     * @return jobs with a matching deadline (may be empty)
     */
    public List<Job> filterJobsByDeadline(String deadline) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null && j.getDeadline().equals(deadline))
                .collect(Collectors.toList());
    }

    /**
     * Returns jobs belonging to a specific module.
     *
     * @param moduleName the module name to match exactly
     * @return jobs for that module (may be empty)
     */
    public List<Job> filterJobsByModule(String moduleName) {
        return jobs.stream()
                .filter(j -> j.getModuleName() != null && j.getModuleName().equals(moduleName))
                .collect(Collectors.toList());
    }

    // ==================== Searching ====================

    /**
     * Searches jobs by keyword across title, description, and module name.
     * <p>
     * Case-insensitive partial match. The keyword is lower-cased before comparison.
     * </p>
     *
     * @param keyword the search term (not null; callers should normalize empty strings)
     * @return matching jobs (may be empty)
     */
    public List<Job> searchJobs(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getTitle().toLowerCase().contains(lowerKeyword) ||
                        (j.getDescription() != null && j.getDescription().toLowerCase().contains(lowerKeyword)) ||
                        (j.getModuleName() != null && j.getModuleName().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * Searches jobs whose required skills contain the given skill (case-insensitive).
     *
     * @param skill the skill substring to match
     * @return jobs with at least one required skill containing the term (may be empty)
     */
    public List<Job> searchJobsBySkill(String skill) {
        String lowerSkill = skill.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getRequiredSkills().stream()
                        .anyMatch(s -> s.toLowerCase().contains(lowerSkill)))
                .collect(Collectors.toList());
    }

    // ==================== Sorting ====================

    /**
     * Sorts all jobs alphabetically by title.
     *
     * @param ascending {@code true} for A–Z, {@code false} for Z–A
     * @return a new sorted list (does not modify the internal cache order)
     */
    public List<Job> sortJobsByTitle(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getTitle) :
                        Comparator.comparing(Job::getTitle).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Sorts all jobs by post date (string comparison; assumes {@code yyyy-MM-dd} format).
     *
     * @param ascending {@code true} for oldest first, {@code false} for newest first
     * @return a new sorted list
     */
    public List<Job> sortJobsByPostDate(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getPostDate) :
                        Comparator.comparing(Job::getPostDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Sorts jobs by deadline. Jobs without a deadline are excluded.
     *
     * @param ascending {@code true} for earliest deadline first
     * @return a new sorted list of jobs that have a deadline
     */
    public List<Job> sortJobsByDeadline(boolean ascending) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null)
                .sorted(ascending ?
                        Comparator.comparing(Job::getDeadline) :
                        Comparator.comparing(Job::getDeadline).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Sorts jobs by the number of remaining available positions
     * ({@code maxPositions - filledPositions}).
     *
     * @param ascending {@code true} for fewest remaining positions first
     * @return a new sorted list
     */
    public List<Job> sortJobsByAvailablePositions(boolean ascending) {
        Comparator<Job> comparator = (j1, j2) -> Integer.compare(
                j1.getMaxPositions() - j1.getFilledPositions(),
                j2.getMaxPositions() - j2.getFilledPositions());
        return jobs.stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .collect(Collectors.toList());
    }

    // ==================== Statistics ====================

    /**
     * Returns a summary map with counts of total, open, closed, and filled jobs.
     *
     * @return a map with keys {@code totalJobs}, {@code openJobs}, {@code closedJobs}, {@code filledJobs}
     */
    public Map<String, Integer> getJobStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalJobs", jobs.size());
        stats.put("openJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).count());
        stats.put("closedJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.CLOSED).count());
        stats.put("filledJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.FILLED).count());
        return stats;
    }

    /**
     * Returns a breakdown of job counts grouped by {@link Job.JobType}.
     *
     * @return a map from each job type to its count (includes zero counts)
     */
    public Map<Job.JobType, Integer> getJobCountByType() {
        Map<Job.JobType, Integer> counts = new HashMap<>();
        for (Job.JobType type : Job.JobType.values()) {
            counts.put(type, (int) jobs.stream().filter(j -> j.getJobType() == type).count());
        }
        return counts;
    }

    /**
     * Sums remaining unfilled positions across every job.
     *
     * @return total available slots ({@code maxPositions - filledPositions} per job)
     */
    public int getTotalAvailablePositions() {
        return jobs.stream()
                .mapToInt(j -> j.getMaxPositions() - j.getFilledPositions())
                .sum();
    }

    /**
     * Sums filled positions across every job.
     *
     * @return total filled slots
     */
    public int getTotalFilledPositions() {
        return jobs.stream()
                .mapToInt(Job::getFilledPositions)
                .sum();
    }

    /**
     * Returns a map of module organiser user IDs to the number of jobs each has posted.
     *
     * @return MO user ID → job count
     */
    public Map<String, Integer> getJobCountByMO() {
        Map<String, Integer> counts = new HashMap<>();
        jobs.forEach(j -> counts.put(j.getPostedBy(), counts.getOrDefault(j.getPostedBy(), 0) + 1));
        return counts;
    }

    // ==================== Expiration Check ====================

    /**
     * Scans all OPEN jobs and closes any whose deadline has passed or is today.
     * <p>
     * For each expired job, sends a {@link Notification.Type#POSITION_EXPIRATION}
     * notification to every applicant who still has a {@link com.recruitment.model.Application.Status#PENDING}
     * application.
     * </p>
     * <p>
     * Called periodically by a background timer in {@link com.recruitment.Main}.
     * Requires both {@code applicationService} and {@code notificationService} to be injected
     * before invocation; otherwise a {@link NullPointerException} may occur.
     * </p>
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
