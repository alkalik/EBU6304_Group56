package com.recruitment.model;

public class Application {
    public enum Status {
        PENDING, ACCEPTED, REJECTED, WITHDRAWN
    }

    private String id;
    private String jobId;
    private String applicantId; // TA user id
    private Status status;
    private String applyDate;
    private String coverLetter;
    private String reviewNote;
    private String reviewedBy;

    public Application() {
        this.status = Status.PENDING;
    }

    public Application(String id, String jobId, String applicantId, String applyDate) {
        this.id = id;
        this.jobId = jobId;
        this.applicantId = applicantId;
        this.applyDate = applyDate;
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getApplyDate() { return applyDate; }
    public void setApplyDate(String applyDate) { this.applyDate = applyDate; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    @Override
    public String toString() {
        return "Application[" + id + "] Job:" + jobId + " Status:" + status;
    }
}
