package com.recruitment.service;

import com.recruitment.model.Application;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class ApplicationServiceTest {
    private ApplicationService applicationService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        applicationService = new ApplicationService();
    }

    @Test
    public void testApply() {
        String jobId = "JOB-" + System.currentTimeMillis();
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
        String jobId = "JOB-dup-" + System.currentTimeMillis();
        String applicantId = "USR-dup-" + System.currentTimeMillis();

        Application first = applicationService.apply(jobId, applicantId, "First");
        assertNotNull(first);

        Application duplicate = applicationService.apply(jobId, applicantId, "Second");
        assertNull("Duplicate application should return null", duplicate);
    }

    @Test
    public void testAcceptApplication() {
        String jobId = "JOB-acc-" + System.currentTimeMillis();
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
        String jobId = "JOB-rej-" + System.currentTimeMillis();
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
        String jobId = "JOB-wd-" + System.currentTimeMillis();
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
        Application a1 = applicationService.apply("JOB-C1", applicantId, "");
        Application a2 = applicationService.apply("JOB-C2", applicantId, "");

        applicationService.acceptApplication(a1.getId(), "R1");

        assertEquals(1, applicationService.getAcceptedCountByApplicant(applicantId));
    }
}
