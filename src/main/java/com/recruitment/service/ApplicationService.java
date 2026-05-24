package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.Notification;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for TA job applications.
 * <p>
 * Handles apply, accept, reject, withdraw, and lookup operations. Coordinates with
 * {@link JobService} to update filled positions and job status, and with
 * {@link NotificationService} to alert module organisers of new applications.
 * </p>
 * <p>
 * Data is persisted in {@code data/applications.json} via {@link JsonUtil}. An in-memory
 * list is loaded at construction and written back on each mutating operation.
 * </p>
 */
public class ApplicationService {
    private static final String FILE_NAME = "applications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Application>>() {}.getType();

    private List<Application> applications;
    private JobService jobService;
    private NotificationService notificationService;

    /**
     * Loads applications from JSON. {@link JobService} and {@link NotificationService}
     * must be injected via setters before apply or accept operations that depend on them.
     */
    public ApplicationService() {
        this.applications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    /**
     * Injects the job service (required for apply notifications and accept logic).
     *
     * @param jobService the {@link JobService} instance to use
     */
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Injects the notification service (required for apply notifications).
     *
     * @param notificationService the {@link NotificationService} instance to use
     */
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Reloads application data from the JSON file, discarding unsaved in-memory changes.
     */
    public void reload() {
        this.applications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, applications);
    }

    /**
     * Submits a new application for a job.
     * <p>
     * Fails if the applicant already has a non-withdrawn application for the same job.
     * Notifies the job's module organiser via {@link Notification.Type#NEW_APPLICATION}.
     * </p>
     *
     * @param jobId        the target job ID
     * @param applicantId  the applying TA's user ID
     * @param coverLetter  optional cover letter text (may be {@code null})
     * @return the created {@link Application} with status {@link Application.Status#PENDING};
     *         {@code null} if the applicant already applied
     */
    public Application apply(String jobId, String applicantId, String coverLetter) {
        // Check if already applied
        boolean alreadyApplied = applications.stream()
                .anyMatch(a -> a.getJobId().equals(jobId)
                        && a.getApplicantId().equals(applicantId)
                        && a.getStatus() != Application.Status.WITHDRAWN);
        if (alreadyApplied) {
            return null;
        }

        Application app = new Application(
                IDGenerator.generate("APP"),
                jobId,
                applicantId,
                LocalDate.now().toString()
        );
        app.setCoverLetter(coverLetter);
        applications.add(app);
        save();

        // Notify MO about new application
        Optional<Job> job = jobService.findById(jobId);
        if (job.isPresent()) {
            notificationService.createNotification(
                job.get().getPostedBy(),
                "New application received for '" + job.get().getTitle() + "'.",
                Notification.Type.NEW_APPLICATION
            );
        }

        return app;
    }

    /**
     * Updates an existing application by matching on {@link Application#getId()}.
     *
     * @param app the application record with updated fields
     * @return {@code true} if found and updated; {@code false} if no matching ID exists
     */
    public boolean updateApplication(Application app) {
        for (int i = 0; i < applications.size(); i++) {
            if (applications.get(i).getId().equals(app.getId())) {
                applications.set(i, app);
                save();
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts a pending application and increments the job's filled position count.
     * <p>
     * Succeeds only when the application is {@link Application.Status#PENDING}, the job
     * is {@link Job.Status#OPEN}, and filled positions are below the maximum. Sets the job
     * to {@link Job.Status#FILLED} when capacity is reached.
     * </p>
     *
     * @param appId      the application ID to accept
     * @param reviewerId the MO user ID performing the review
     * @return {@code true} if accepted and persisted; {@code false} on invalid state or missing records
     */
    public boolean acceptApplication(String appId, String reviewerId) {
        Optional<Application> app = findById(appId);
        if (!app.isPresent()) {
            return false;
        }
        Application target = app.get();
        if (target.getStatus() != Application.Status.PENDING) {
            return false;
        }

        Optional<Job> jobOpt = jobService.findById(target.getJobId());
        if (!jobOpt.isPresent()) {
            return false;
        }
        Job job = jobOpt.get();
        if (job.getStatus() != Job.Status.OPEN || job.getFilledPositions() >= job.getMaxPositions()) {
            return false;
        }

        target.setStatus(Application.Status.ACCEPTED);
        target.setReviewedBy(reviewerId);
        job.setFilledPositions(job.getFilledPositions() + 1);
        if (job.getFilledPositions() >= job.getMaxPositions()) {
            job.setStatus(Job.Status.FILLED);
        }
        save();
        jobService.updateJob(job);
        return true;
    }

    /**
     * Rejects a pending application and records reviewer notes.
     *
     * @param appId      the application ID to reject
     * @param reviewerId the MO user ID performing the review
     * @param note       optional rejection note (may be {@code null})
     * @return {@code true} if rejected and persisted; {@code false} if not found or not pending
     */
    public boolean rejectApplication(String appId, String reviewerId, String note) {
        Optional<Application> app = findById(appId);
        if (!app.isPresent()) {
            return false;
        }
        Application target = app.get();
        if (target.getStatus() != Application.Status.PENDING) {
            return false;
        }
        target.setStatus(Application.Status.REJECTED);
        target.setReviewedBy(reviewerId);
        target.setReviewNote(note);
        save();
        return true;
    }

    /**
     * Withdraws a pending application by the applicant.
     * <p>
     * Sets {@link Application#setWithdrawnAt(String)} to the current date-time.
     * </p>
     *
     * @param appId the application ID to withdraw
     * @return {@code true} if withdrawn and persisted; {@code false} if not found or not pending
     */
    public boolean withdrawApplication(String appId) {
        Optional<Application> app = findById(appId);
        if (!app.isPresent()) {
            return false;
        }
        Application target = app.get();
        if (target.getStatus() != Application.Status.PENDING) {
            return false;
        }
        target.setStatus(Application.Status.WITHDRAWN);
        target.setWithdrawnAt(LocalDateTime.now().toString());
        save();
        return true;
    }

    /**
     * Finds an application by unique identifier.
     *
     * @param id the application ID (e.g. {@code APP-...})
     * @return an {@link Optional} containing the application if found, or empty otherwise
     */
    public Optional<Application> findById(String id) {
        return applications.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    /**
     * Returns all applications submitted by a given TA.
     *
     * @param applicantId the TA's user ID
     * @return a list of applications for that applicant (may be empty)
     */
    public List<Application> getApplicationsByApplicant(String applicantId) {
        return applications.stream()
                .filter(a -> a.getApplicantId().equals(applicantId))
                .collect(Collectors.toList());
    }

    /**
     * Returns all applications for a given job posting.
     *
     * @param jobId the job ID
     * @return a list of applications for that job (may be empty)
     */
    public List<Application> getApplicationsByJob(String jobId) {
        return applications.stream()
                .filter(a -> a.getJobId().equals(jobId))
                .collect(Collectors.toList());
    }

    /**
     * Returns the live in-memory list of all applications.
     *
     * @return the internal list of all applications (never {@code null})
     */
    public List<Application> getAllApplications() {
        return applications;
    }

    /**
     * Counts how many applications a TA has in {@link Application.Status#ACCEPTED} status.
     *
     * @param applicantId the TA's user ID
     * @return the number of accepted applications
     */
    public long getAcceptedCountByApplicant(String applicantId) {
        return applications.stream()
                .filter(a -> a.getApplicantId().equals(applicantId)
                        && a.getStatus() == Application.Status.ACCEPTED)
                .count();
    }
}
