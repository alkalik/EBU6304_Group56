package com.recruitment.service;

import com.recruitment.model.Job;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JobServiceTest {
    private JobService jobService;

    @Before
    public void setUp() {
        new File("data").mkdirs();
        jobService = new JobService();
    }

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

    @Test
    public void testSortJobsByTitle() {
        List<Job> sorted = jobService.sortJobsByTitle(true);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getTitle().compareTo(sorted.get(i + 1).getTitle()) <= 0);
        }
    }

    @Test
    public void testJobStatistics() {
        Map<String, Integer> stats = jobService.getJobStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalJobs"));
        assertTrue(stats.containsKey("openJobs"));
        assertTrue(stats.get("totalJobs") >= 0);
    }

    @Test
    public void testGetJobCountByType() {
        Map<Job.JobType, Integer> counts = jobService.getJobCountByType();
        assertNotNull(counts);
        assertTrue(counts.containsKey(Job.JobType.MODULE_TA));
        assertTrue(counts.containsKey(Job.JobType.INVIGILATION));
        assertTrue(counts.containsKey(Job.JobType.OTHER));
    }

    @Test
    public void testGetTotalAvailablePositions() {
        int available = jobService.getTotalAvailablePositions();
        assertTrue(available >= 0);
    }
}

