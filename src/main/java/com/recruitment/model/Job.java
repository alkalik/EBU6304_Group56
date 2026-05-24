package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for a teaching-assistant or invigilation vacancy posted by a module organiser.
 * <p>
 * Tracks posting metadata, required skills, capacity ({@code maxPositions} /
 * {@code filledPositions}), and lifecycle {@link Status}. Persisted via {@code jobs.json}.
 */
public class Job {

    /**
     * Category of work offered by the vacancy.
     */
    public enum JobType {
        /** Module teaching-assistant duties. */
        MODULE_TA,
        /** Examination or session invigilation. */
        INVIGILATION,
        /** Other departmental support roles. */
        OTHER
    }

    /**
     * Publication and hiring state of a job posting.
     */
    public enum Status {
        /** Accepting applications. */
        OPEN,
        /** No longer accepting applications (deadline passed or manually closed). */
        CLOSED,
        /** All allocated positions have been filled. */
        FILLED
    }

    private String id;
    private String title;
    private String description;
    private String moduleName;
    private String postedBy; // MO user id
    private JobType jobType;
    private Status status;
    private List<String> requiredSkills;
    private int maxPositions;
    private int filledPositions;
    private String semester;
    private String postDate;
    private String deadline;

    /**
     * Creates a job with default {@link Status#OPEN}, zero filled positions,
     * and an empty required-skills list.
     */
    public Job() {
        this.requiredSkills = new ArrayList<>();
        this.status = Status.OPEN;
        this.filledPositions = 0;
    }

    /**
     * Creates a job with the given core fields and default open status.
     *
     * @param id           unique job identifier
     * @param title        short vacancy title
     * @param description  full role description
     * @param postedBy     user id of the module organiser who created the posting
     * @param jobType      category of work
     * @param maxPositions maximum number of hires for this posting
     */
    public Job(String id, String title, String description, String postedBy, JobType jobType, int maxPositions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.postedBy = postedBy;
        this.jobType = jobType;
        this.maxPositions = maxPositions;
        this.requiredSkills = new ArrayList<>();
        this.status = Status.OPEN;
        this.filledPositions = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public int getMaxPositions() { return maxPositions; }
    public void setMaxPositions(int maxPositions) { this.maxPositions = maxPositions; }

    public int getFilledPositions() { return filledPositions; }
    public void setFilledPositions(int filledPositions) { this.filledPositions = filledPositions; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getPostDate() { return postDate; }
    public void setPostDate(String postDate) { this.postDate = postDate; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    /**
     * Returns a short display label combining title and status.
     *
     * @return formatted string such as {@code "CS101 TA (OPEN)"}
     */
    @Override
    public String toString() {
        return title + " (" + status + ")";
    }
}
