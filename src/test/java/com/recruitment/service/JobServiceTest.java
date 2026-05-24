package com.recruitment.service;

import com.recruitment.model.Job;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JobService} CRUD, search, filter, sort, and statistics operations.
 */
public class JobServiceTest {
    private JobService jobService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        jobService = new JobService();
    }

    /**
     * Verifies that creating a job assigns an ID, OPEN status, and a post date.
     */
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

    /**
     * Verifies that {@code getOpenJobs} returns only jobs with OPEN status.
     */
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

    /**
     * Verifies that closing a job by ID updates its status to CLOSED.
     */
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

    /**
     * Verifies that jobs posted by a specific module officer are returned for that MO ID.
     */
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

    /**
     * Verifies that deleting a job removes it from lookup by ID.
     */
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

    /**
     * Verifies that filtering by status and job type returns only matching jobs.
     */
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

    /**
     * Verifies that keyword search finds jobs whose title contains the search term.
     */
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

    /**
     * Verifies that skill-based search returns jobs listing the given required skill.
     */
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

    /**
     * Verifies that jobs sorted by title ascending are in non-decreasing title order.
     */
    @Test
    public void testSortJobsByTitle() {
        List<Job> sorted = jobService.sortJobsByTitle(true);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getTitle().compareTo(sorted.get(i + 1).getTitle()) <= 0);
        }
    }

    /**
     * Verifies that job statistics include total and open job counts with non-negative values.
     */
    @Test
    public void testJobStatistics() {
        Map<String, Integer> stats = jobService.getJobStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalJobs"));
        assertTrue(stats.containsKey("openJobs"));
        assertTrue(stats.get("totalJobs") >= 0);
    }

    /**
     * Verifies that job counts by type include all defined {@link Job.JobType} values.
     */
    @Test
    public void testGetJobCountByType() {
        Map<Job.JobType, Integer> counts = jobService.getJobCountByType();
        assertNotNull(counts);
        assertTrue(counts.containsKey(Job.JobType.MODULE_TA));
        assertTrue(counts.containsKey(Job.JobType.INVIGILATION));
        assertTrue(counts.containsKey(Job.JobType.OTHER));
    }

    /**
     * Verifies that total available positions across open jobs is non-negative.
     */
    @Test
    public void testGetTotalAvailablePositions() {
        int available = jobService.getTotalAvailablePositions();
        assertTrue(available >= 0);
    }
}
