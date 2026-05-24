package com.recruitment.service;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.Notification;
import com.recruitment.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RecruitmentPipelineTest extends ServiceTestBase {
    private UserService userService;
    private JobService jobService;
    private ApplicationService applicationService;
    private NotificationService notificationService;
    private AIAnalysisService aiAnalysisService;

    @Before
    public void setUp() {
        ServiceGraph services = newServiceGraph();
        userService = services.userService;
        jobService = services.jobService;
        applicationService = services.applicationService;
        notificationService = services.notificationService;
        aiAnalysisService = new AIAnalysisService();
    }

    @Test
    public void fullRecruitmentPipelineCreatesReviewsAndAcceptsApplication() {
        User mo = registerUser("pipeline_mo", User.Role.MO, "Pipeline MO",
                "pipeline.mo@test.com", "Software Engineering", null);
        User strongTa = registerUser("pipeline_ta_strong", User.Role.TA, "Strong TA",
                "strong.ta@test.com", "Computer Science", Arrays.asList("Java", "Agile", "JUnit"));
        User partialTa = registerUser("pipeline_ta_partial", User.Role.TA, "Partial TA",
                "partial.ta@test.com", "Computer Science", Arrays.asList("Java"));

        Job job = new Job();
        job.setTitle("Software Engineering Lab TA");
        job.setDescription("Support labs, code reviews, and coursework feedback.");
        job.setModuleName("EBU6304");
        job.setPostedBy(mo.getId());
        job.setJobType(Job.JobType.MODULE_TA);
        job.setRequiredSkills(Arrays.asList("Java", "Agile", "JUnit"));
        job.setMaxPositions(1);

        Job createdJob = jobService.createJob(job);
        assertNotNull(createdJob.getId());
        assertEquals(Job.Status.OPEN, createdJob.getStatus());

        Application strongApplication = applicationService.apply(createdJob.getId(), strongTa.getId(), "I can support Java labs.");
        Application partialApplication = applicationService.apply(createdJob.getId(), partialTa.getId(), "I have Java experience.");

        assertNotNull(strongApplication);
        assertNotNull(partialApplication);
        assertEquals(Application.Status.PENDING, strongApplication.getStatus());
        assertEquals(Application.Status.PENDING, partialApplication.getStatus());

        List<Notification> moNotifications = notificationService.getNotificationsByUser(mo.getId());
        assertEquals(2, moNotifications.size());
        assertTrue(moNotifications.stream().allMatch(n -> n.getType() == Notification.Type.NEW_APPLICATION));

        List<AIAnalysisService.CandidateAnalysis> analysis =
                aiAnalysisService.analyzeJobApplicants(createdJob,
                        applicationService.getApplicationsByJob(createdJob.getId()),
                        userService);

        assertEquals(2, analysis.size());
        assertEquals(strongTa.getId(), analysis.get(0).ta.getId());
        assertEquals(100.0, analysis.get(0).matchPercent, 0.001);
        assertEquals(partialTa.getId(), analysis.get(1).ta.getId());
        assertEquals(1, analysis.get(1).matchedSkills.size());
        assertEquals(2, analysis.get(1).missingSkills.size());

        assertTrue(applicationService.acceptApplication(strongApplication.getId(), mo.getId()));
        assertFalse(applicationService.acceptApplication(partialApplication.getId(), mo.getId()));

        Application accepted = applicationService.findById(strongApplication.getId()).orElse(null);
        Application stillPending = applicationService.findById(partialApplication.getId()).orElse(null);
        Job filledJob = jobService.findById(createdJob.getId()).orElse(null);

        assertNotNull(accepted);
        assertNotNull(stillPending);
        assertNotNull(filledJob);
        assertEquals(Application.Status.ACCEPTED, accepted.getStatus());
        assertEquals(Application.Status.PENDING, stillPending.getStatus());
        assertEquals(1, filledJob.getFilledPositions());
        assertEquals(Job.Status.FILLED, filledJob.getStatus());

        AIAnalysisService.WorkloadAnalysisResult workload =
                aiAnalysisService.analyzeWorkload(Arrays.asList(strongTa, partialTa), applicationService, jobService);

        assertEquals(2, workload.workloads.size());
        assertTrue(workload.summary.contains("Total TAs: 2"));
        assertTrue(workload.workloads.stream()
                .anyMatch(w -> w.ta.getId().equals(strongTa.getId())
                        && w.acceptedJobs == 1
                        && w.workloadScore == 3.0));
        assertTrue(workload.workloads.stream()
                .anyMatch(w -> w.ta.getId().equals(partialTa.getId())
                        && w.pendingApps == 1
                        && w.workloadScore == 1.0));
    }

    private User registerUser(String username, User.Role role, String name,
                              String email, String department, List<String> skills) {
        User user = new User(null, username, "pass123", role, name, email);
        user.setDepartment(department);
        if (skills != null) {
            user.setSkills(skills);
        }
        assertTrue(userService.register(user));
        return user;
    }
}
