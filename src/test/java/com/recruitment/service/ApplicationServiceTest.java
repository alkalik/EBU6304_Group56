package com.recruitment.service;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class ApplicationServiceTest {
    private ApplicationService applicationService;
    private JobService jobService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        jobService = new JobService();
        applicationService = new ApplicationService();
        applicationService.setJobService(jobService);
    }

    private Job createOpenJob(int maxPositions) {
        Job job = new Job();
        job.setTitle("Job-" + System.currentTimeMillis());
        job.setPostedBy("MO-" + System.currentTimeMillis());
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(maxPositions);
        return jobService.createJob(job);
    }

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

    @Test
    public void testDuplicateApplication() {
        String jobId = createOpenJob(2).getId();
        String applicantId = "USR-dup-" + System.currentTimeMillis();

        Application first = applicationService.apply(jobId, applicantId, "First");
        assertNotNull(first);

        Application duplicate = applicationService.apply(jobId, applicantId, "Second");
        assertNull("Duplicate application should return null", duplicate);
    }

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

    @Test
    public void testGetApplicationsByApplicant() {
        String applicantId = "USR-list-" + System.currentTimeMillis();
        applicationService.apply("JOB-A", applicantId, "A");
        applicationService.apply("JOB-B", applicantId, "B");

        List<Application> apps = applicationService.getApplicationsByApplicant(applicantId);
        assertEquals(2, apps.size());
        assertTrue(apps.stream().allMatch(a -> a.getApplicantId().equals(applicantId)));
    }

    @Test
    public void testGetAcceptedCount() {
        String applicantId = "USR-cnt-" + System.currentTimeMillis();
        Application a1 = applicationService.apply(createOpenJob(1).getId(), applicantId, "");
        Application a2 = applicationService.apply(createOpenJob(1).getId(), applicantId, "");
        assertNotNull(a2);

        applicationService.acceptApplication(a1.getId(), "R1");

        assertEquals(1, applicationService.getAcceptedCountByApplicant(applicantId));
    }

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
