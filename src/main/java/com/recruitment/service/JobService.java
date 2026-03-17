package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Job;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JobService {
    private static final String FILE_NAME = "jobs.json";
    private static final Type LIST_TYPE = new TypeToken<List<Job>>() {}.getType();

    private List<Job> jobs;

    public JobService() {
        this.jobs = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    public void reload() {
        this.jobs = JsonUtil.loadList(FILE_NAME, LIST_TYPE);
    }

    private void save() {
        JsonUtil.saveList(FILE_NAME, jobs);
    }

    public Job createJob(Job job) {
        job.setId(IDGenerator.generate("JOB"));
        job.setPostDate(LocalDate.now().toString());
        job.setStatus(Job.Status.OPEN);
        jobs.add(job);
        save();
        return job;
    }

    public boolean updateJob(Job job) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId().equals(job.getId())) {
                jobs.set(i, job);
                save();
                return true;
            }
        }
        return false;
    }

    public Optional<Job> findById(String id) {
        return jobs.stream().filter(j -> j.getId().equals(id)).findFirst();
    }

    public List<Job> getOpenJobs() {
        return jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).collect(Collectors.toList());
    }

    public List<Job> getJobsByMO(String moId) {
        return jobs.stream().filter(j -> j.getPostedBy().equals(moId)).collect(Collectors.toList());
    }

    public List<Job> getAllJobs() {
        return jobs;
    }

    public boolean deleteJob(String id) {
        boolean removed = jobs.removeIf(j -> j.getId().equals(id));
        if (removed) save();
        return removed;
    }

    public boolean closeJob(String id) {
        Optional<Job> job = findById(id);
        if (job.isPresent()) {
            job.get().setStatus(Job.Status.CLOSED);
            save();
            return true;
        }
        return false;
    }
}
