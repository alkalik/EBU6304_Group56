package com.recruitment.service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.recruitment.model.Job;

public class JobServiceTest {
    private JobService jobService;

    // Initializes the JobService instance and ensures the storage directory exists before each test running
    @Before
    public void setUp() {
        new File("data").mkdirs();
        jobService = new JobService();
    }

    // Verifies that a new job posting can be successfully created with default OPEN status and a post date
    @Test
    public void testCreateJob() {
        Job job = new Job();
        job.setTitle("TA for Software Engineering");
        job.setDescription("Assist in lab sessions");
        job.setModuleName("EBU6304");
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(3);
        job.setRequiredSkills(Arrays.asList("Java", "Agile"));

        Job created = jobService.createJob(job);
        assertNotNull(created.getId());
        assertEquals(Job.Status.OPEN, created.getStatus());
        assertNotNull(created.getPostDate());
    }

    // Ensures that the system can successfully fetch all job postings that are currently active and open
    @Test
    public void testGetOpenJobs() {
        Job job = new Job();
        job.setTitle("Open Job " + System.currentTimeMillis());
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(1);
        jobService.createJob(job);

        List<Job> openJobs = jobService.getOpenJobs();
        assertFalse(openJobs.isEmpty());
        assertTrue(openJobs.stream().allMatch(j -> j.getStatus() == Job.Status.OPEN));
    }

    // Checks that an open job can be manually closed, changing its state to CLOSED
    @Test
    public void testCloseJob() {
        Job job = new Job();
        job.setTitle("ToClose " + System.currentTimeMillis());
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.INVIGILATION);
        job.setMaxPositions(2);
        jobService.createJob(job);

        assertTrue(jobService.closeJob(job.getId()));
        assertEquals(Job.Status.CLOSED, jobService.findById(job.getId()).get().getStatus());
    }

    // Verifies that a specific Module Organizer (MO) can retrieve all the jobs they have personally posted
    @Test
    public void testGetJobsByMO() {
        String moId = "MO-" + System.currentTimeMillis();
        Job job = new Job();
        job.setTitle("MO Job");
        job.setPostedBy(moId);
        job.setJobType(Job.JobType.OTHER);
        job.setMaxPositions(1);
        jobService.createJob(job);

        List<Job> jobs = jobService.getJobsByMO(moId);
        assertFalse(jobs.isEmpty());
        assertTrue(jobs.stream().allMatch(j -> j.getPostedBy().equals(moId)));
    }

    // Validates that a job can be fully deleted and removed from the underlying data storage
    @Test
    public void testDeleteJob() {
        Job job = new Job();
        job.setTitle("ToDelete " + System.currentTimeMillis());
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(1);
        jobService.createJob(job);

        assertTrue(jobService.deleteJob(job.getId()));
        assertFalse(jobService.findById(job.getId()).isPresent());
    }

    // Tests the capability to filter jobs simultaneously by both their operational status and recruitment type
    @Test
    public void testFilterJobs() {
        Job job1 = new Job();
        job1.setTitle("Filter Test 1 " + System.currentTimeMillis());
        job1.setPostedBy("MO-001");
        job1.setJobType(Job.JobType.MODULE_TA);
        job1.setMaxPositions(1);
        jobService.createJob(job1);

        List<Job> filtered = jobService.filterJobs(Job.Status.OPEN, Job.JobType.MODULE_TA);
        assertTrue(filtered.stream().allMatch(j -> j.getStatus() == Job.Status.OPEN && j.getJobType() == Job.JobType.MODULE_TA));
    }

    // Verifies text-based keyword search targeting core job properties like the title or description text
    @Test
    public void testSearchJobs() {
        Job job = new Job();
        job.setTitle("Java Developer " + System.currentTimeMillis());
        job.setDescription("Develop Java applications");
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(1);
        jobService.createJob(job);

        List<Job> results = jobService.searchJobs("Java");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(j -> j.getTitle().contains("Java")));
    }

    // Verifies that applicants can look up matching jobs based on exact technical skill prerequisites
    @Test
    public void testSearchJobsBySkill() {
        Job job = new Job();
        job.setTitle("Skill Test " + System.currentTimeMillis());
        job.setPostedBy("MO-001");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(1);
        job.setRequiredSkills(Arrays.asList("Python", "Django"));
        jobService.createJob(job);

        List<Job> results = jobService.searchJobsBySkill("Python");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(j -> j.getRequiredSkills().contains("Python")));
    }

    // Checks if the sorting algorithm correctly arranges job lists in alphabetical order based on title
    @Test
    public void testSortJobsByTitle() {
        List<Job> sorted = jobService.sortJobsByTitle(true);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getTitle().compareTo(sorted.get(i + 1).getTitle()) <= 0);
        }
    }

    // Confirms basic system-wide dashboard counters like total records vs. open positions are properly grouped
    @Test
    public void testJobStatistics() {
        Map<String, Integer> stats = jobService.getJobStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalJobs"));
        assertTrue(stats.containsKey("openJobs"));
        assertTrue(stats.get("totalJobs") >= 0);
    }

    // Assures job categories (TA, Invigilation, etc.) map successfully to their respective occurrence counts
    @Test
    public void testGetJobCountByType() {
        Map<Job.JobType, Integer> counts = jobService.getJobCountByType();
        assertNotNull(counts);
        assertTrue(counts.containsKey(Job.JobType.MODULE_TA));
        assertTrue(counts.containsKey(Job.JobType.INVIGILATION));
        assertTrue(counts.containsKey(Job.JobType.OTHER));
    }

    // Checks that the aggregate sum of unfulfilled seats across all job offers remains a non-negative number
    @Test
    public void testGetTotalAvailablePositions() {
        int available = jobService.getTotalAvailablePositions();
        assertTrue(available >= 0);
    }
}