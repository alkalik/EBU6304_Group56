package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.Notification;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationService {
    private static final String FILE_NAME = "applications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Application>>() {}.getType();

    private List<Application> applications;
    private NotificationService notificationService;
    private JobService jobService;

    public ApplicationService() {
        this.applications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
        this.notificationService = new NotificationService();
        this.jobService = new JobService();
    }

    public void reload() {
        this.applications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, applications);
    }

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

    public boolean acceptApplication(String appId, String reviewerId) {
        Optional<Application> app = findById(appId);
        if (app.isPresent()) {
            app.get().setStatus(Application.Status.ACCEPTED);
            app.get().setReviewedBy(reviewerId);
            save();
            // Notify TA about status update
            Optional<Job> job = jobService.findById(app.get().getJobId());
            String jobTitle = job.isPresent() ? job.get().getTitle() : "Unknown Job";
            notificationService.createNotification(
                app.get().getApplicantId(),
                "Your application for '" + jobTitle + "' has been accepted.",
                Notification.Type.APPLICATION_STATUS_UPDATE
            );
            return true;
        }
        return false;
    }

    public boolean rejectApplication(String appId, String reviewerId, String note) {
        Optional<Application> app = findById(appId);
        if (app.isPresent()) {
            app.get().setStatus(Application.Status.REJECTED);
            app.get().setReviewedBy(reviewerId);
            app.get().setReviewNote(note);
            save();
            // Notify TA about status update
            Optional<Job> job = jobService.findById(app.get().getJobId());
            String jobTitle = job.isPresent() ? job.get().getTitle() : "Unknown Job";
            notificationService.createNotification(
                app.get().getApplicantId(),
                "Your application for '" + jobTitle + "' has been rejected.",
                Notification.Type.APPLICATION_STATUS_UPDATE
            );
            return true;
        }
        return false;
    }

    public boolean withdrawApplication(String appId) {
        Optional<Application> app = findById(appId);
        if (app.isPresent()) {
            app.get().setStatus(Application.Status.WITHDRAWN);
            save();
            // Notify TA about withdrawal success
            Optional<Job> job = jobService.findById(app.get().getJobId());
            String jobTitle = job.isPresent() ? job.get().getTitle() : "Unknown Job";
            notificationService.createNotification(
                app.get().getApplicantId(),
                "Your application for '" + jobTitle + "' has been successfully withdrawn.",
                Notification.Type.WITHDRAWAL_SUCCESS
            );
            return true;
        }
        return false;
    }

    public Optional<Application> findById(String id) {
        return applications.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    public List<Application> getApplicationsByApplicant(String applicantId) {
        return applications.stream()
                .filter(a -> a.getApplicantId().equals(applicantId))
                .collect(Collectors.toList());
    }

    public List<Application> getApplicationsByJob(String jobId) {
        return applications.stream()
                .filter(a -> a.getJobId().equals(jobId))
                .collect(Collectors.toList());
    }

    public List<Application> getAllApplications() {
        return applications;
    }

    public long getAcceptedCountByApplicant(String applicantId) {
        return applications.stream()
                .filter(a -> a.getApplicantId().equals(applicantId)
                        && a.getStatus() == Application.Status.ACCEPTED)
                .count();
    }
}
