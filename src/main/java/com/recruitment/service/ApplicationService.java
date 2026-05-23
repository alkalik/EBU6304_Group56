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

public class ApplicationService {
    private static final String FILE_NAME = "applications.json";
    private static final Type LIST_TYPE = new TypeToken<List<Application>>() {}.getType();

    private List<Application> applications;
    private JobService jobService;
    private NotificationService notificationService;

    public ApplicationService() {
        this.applications = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
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
