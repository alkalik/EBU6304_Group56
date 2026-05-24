package com.recruitment.service;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.recruitment.model.Application;
import com.recruitment.model.Job;

/**
 * Integration-style unit tests for {@link ApplicationService} application lifecycle,
 * listing, acceptance limits, and interaction with {@link JobService}.
 */
public class ApplicationServiceTest {
    private ApplicationService applicationService;
    private JobService jobService;

    // Initializes services and ensures the local data directory exists before each test execution
    @Before
    public void setUp() {
        new File("data").mkdirs();
        jobService = new JobService();
        applicationService = new ApplicationService();
        applicationService.setJobService(jobService);
    }

    // Helper method to create and persist a new job with specific capacity for testing
    private Job createOpenJob(int maxPositions) {
        Job job = new Job();
        job.setTitle("Job-" + System.currentTimeMillis());
        job.setPostedBy("MO-" + System.currentTimeMillis());
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(maxPositions);
        return jobService.createJob(job);
    }

    /**
     * Verifies that applying to a job creates a pending application with correct job and applicant IDs.
     */
    // Verifies that a valid job application can be submitted successfully with default PENDING status
    @Test
    public void testApply() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-" + System.currentTimeMillis();

        Application app = applicationService.apply(jobId, applicantId, "I am interested.");
        assertNotNull(app);
        assertNotNull(app.getId());
        assertEquals(Application.Status.PENDING, app.getStatus());
        assertEquals(jobId, app.getJobId());
        assertEquals(applicantId, app.getApplicantId());
    }

    /**
     * Verifies that a second application by the same applicant to the same job returns null.
     */
    // Ensures that an applicant cannot apply to the exact same job multiple times
    @Test
    public void testDuplicateApplication() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-dup-" + System.currentTimeMillis();

        Application first = applicationService.apply(jobId, applicantId, "First");
        assertNotNull(first);

        Application duplicate = applicationService.apply(jobId, applicantId, "Second");
        assertNull("Duplicate application should return null", duplicate);
    }

    /**
     * Verifies that accepting an application sets status to ACCEPTED and records the reviewer.
     */
    // Checks that an application can be approved by a reviewer and its status transitions to ACCEPTED
    @Test
    public void testAcceptApplication() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-acc-" + System.currentTimeMillis();

        Application app = applicationService.apply(jobId, applicantId, "");
        assertTrue(applicationService.acceptApplication(app.getId(), "REVIEWER-001"));

        Application updated = applicationService.findById(app.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(Application.Status.ACCEPTED, updated.getStatus());
        assertEquals("REVIEWER-001", updated.getReviewedBy());
    }

    /**
     * Verifies that rejecting an application sets status to REJECTED and stores the review note.
     */
    // Checks that an application can be denied and includes the reviewer's justification note
    @Test
    public void testRejectApplication() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-rej-" + System.currentTimeMillis();

        Application app = applicationService.apply(jobId, applicantId, "");
        assertTrue(applicationService.rejectApplication(app.getId(), "REVIEWER-001", "Not qualified"));

        Application updated = applicationService.findById(app.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(Application.Status.REJECTED, updated.getStatus());
        assertEquals("Not qualified", updated.getReviewNote());
    }

    /**
     * Verifies that withdrawing an application sets status to WITHDRAWN.
     */
    // Verifies that applicants can cancel their own pending applications, changing the status to WITHDRAWN
    @Test
    public void testWithdrawApplication() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-wd-" + System.currentTimeMillis();

        Application app = applicationService.apply(jobId, applicantId, "");
        assertTrue(applicationService.withdrawApplication(app.getId()));

        Application updated = applicationService.findById(app.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(Application.Status.WITHDRAWN, updated.getStatus());
    }

    /**
     * Verifies that all applications for a given applicant are returned and belong to that applicant.
     */
    // Verifies retrieval of all historical applications tied to a specific applicant account
    @Test
    public void testGetApplicationsByApplicant() {
        String applicantId = "USR-list-" + System.currentTimeMillis();
        applicationService.apply("JOB-A", applicantId, "A");
        applicationService.apply("JOB-B", applicantId, "B");

        List<Application> apps = applicationService.getApplicationsByApplicant(applicantId);
        assertEquals(2, apps.size());
        assertTrue(apps.stream().allMatch(a -> a.getApplicantId().equals(applicantId)));
    }

    /**
     * Verifies that {@code getAcceptedCountByApplicant} counts only accepted applications for an applicant.
     */
    // Assures the counter properly returns the number of applications that successfully reached ACCEPTED status
    @Test
    public void testGetAcceptedCount() {
        String applicantId = "USR-cnt-" + System.currentTimeMillis();
        Application a1 = applicationService.apply(createOpenJob(1).getId(), applicantId, "");
        Application a2 = applicationService.apply(createOpenJob(1).getId(), applicantId, "");
        assertNotNull(a2);

        applicationService.acceptApplication(a1.getId(), "R1");

        assertEquals(1, applicationService.getAcceptedCountByApplicant(applicantId));
    }

    /**
     * Verifies that accepting an application increments filled positions and marks a single-position job as FILLED.
     */
    // Assures that accepting an applicant increases filled counts and flips job status to FILLED when limits are met
    @Test
    public void testAcceptApplicationFillsPositionAndMarksJobFilled() {
        Job job = createOpenJob(1);
        String applicantId = "USR-fill-" + System.currentTimeMillis();
        Application app = applicationService.apply(job.getId(), applicantId, "");
        assertNotNull(app);

        assertTrue(applicationService.acceptApplication(app.getId(), "REVIEWER-001"));

        Job updatedJob = jobService.findById(job.getId()).orElse(null);
        assertNotNull(updatedJob);
        assertEquals(1, updatedJob.getFilledPositions());
        assertEquals(Job.Status.FILLED, updatedJob.getStatus());
    }

    /**
     * Verifies that accepting a second applicant fails when the job has only one position and the first remains pending.
     */
    // Confirms that once a job is at maximum capacity, subsequent applications cannot be accepted
    @Test
    public void testAcceptApplicationFailsWhenJobIsFull() {
        Job job = createOpenJob(1);
        Application first = applicationService.apply(job.getId(), "USR-full-1-" + System.currentTimeMillis(), "");
        Application second = applicationService.apply(job.getId(), "USR-full-2-" + System.currentTimeMillis(), "");
        assertNotNull(first);
        assertNotNull(second);

        assertTrue(applicationService.acceptApplication(first.getId(), "R1"));
        assertFalse(applicationService.acceptApplication(second.getId(), "R2"));

        Application secondUpdated = applicationService.findById(second.getId()).orElse(null);
        assertNotNull(secondUpdated);
        assertEquals(Application.Status.PENDING, secondUpdated.getStatus());
    }

    /**
     * Verifies that rejecting an already accepted application fails and leaves status ACCEPTED.
     */
    // Validates business workflow state invariants: an application already ACCEPTED cannot be changed to REJECTED
    @Test
    public void testRejectFailsAfterAccepted() {
        Job job = createOpenJob(2);
        Application app = applicationService.apply(job.getId(), "USR-status-" + System.currentTimeMillis(), "");
        assertNotNull(app);
        assertTrue(applicationService.acceptApplication(app.getId(), "R1"));

        assertFalse(applicationService.rejectApplication(app.getId(), "R2", "late change"));

        Application updated = applicationService.findById(app.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(Application.Status.ACCEPTED, updated.getStatus());
    }
}