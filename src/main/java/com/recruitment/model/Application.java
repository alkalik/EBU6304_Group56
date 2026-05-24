package com.recruitment.model;

/**
 * Domain model linking a teaching assistant applicant to a job posting.
 * <p>
 * Holds application workflow state ({@link Status}), optional cover letter,
 * reviewer feedback, and withdrawal timestamp. Persisted via {@code applications.json}.
 */
public class Application {

    /**
     * Lifecycle state of a job application from submission through resolution.
     */
    public enum Status {
        /** Submitted and awaiting module organiser review. */
        PENDING,
        /** Approved by the reviewer. */
        ACCEPTED,
        /** Declined by the reviewer. */
        REJECTED,
        /** Voluntarily withdrawn by the applicant. */
        WITHDRAWN
    }

    private String id;
    private String jobId;
    private String applicantId; // TA user id
    private Status status;
    private String applyDate;
    private String coverLetter;
    private String reviewNote;
    private String reviewedBy;
    private String withdrawnAt;

    /** Creates an application defaulting to {@link Status#PENDING}. */
    public Application() {
        this.status = Status.PENDING;
    }

    /**
     * Creates a pending application for the given job and applicant.
     *
     * @param id          unique application identifier
     * @param jobId       target job identifier
     * @param applicantId teaching assistant user identifier
     * @param applyDate   submission date string (application-defined format)
     */
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

    public String getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(String withdrawnAt) { this.withdrawnAt = withdrawnAt; }

    /**
     * Returns a diagnostic summary including id, job id, and status.
     *
     * @return formatted application summary string
     */
    @Override
    public String toString() {
        return "Application[" + id + "] Job:" + jobId + " Status:" + status;
    }
}
