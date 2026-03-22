package com.recruitment.service;

import com.google.gson.reflect.TypeToken;
import com.recruitment.model.Job;
import com.recruitment.util.IDGenerator;
import com.recruitment.util.JsonUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // 职位过滤功能
    public List<Job> filterJobs(Job.Status status, Job.JobType jobType) {
        return jobs.stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> jobType == null || j.getJobType() == jobType)
                .collect(Collectors.toList());
    }

    public List<Job> filterJobsByDeadline(String deadline) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null && j.getDeadline().equals(deadline))
                .collect(Collectors.toList());
    }

    public List<Job> filterJobsByModule(String moduleName) {
        return jobs.stream()
                .filter(j -> j.getModuleName() != null && j.getModuleName().equals(moduleName))
                .collect(Collectors.toList());
    }

    // 职位搜索功能
    public List<Job> searchJobs(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getTitle().toLowerCase().contains(lowerKeyword) ||
                        (j.getDescription() != null && j.getDescription().toLowerCase().contains(lowerKeyword)) ||
                        (j.getModuleName() != null && j.getModuleName().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    public List<Job> searchJobsBySkill(String skill) {
        String lowerSkill = skill.toLowerCase();
        return jobs.stream()
                .filter(j -> j.getRequiredSkills().stream()
                        .anyMatch(s -> s.toLowerCase().contains(lowerSkill)))
                .collect(Collectors.toList());
    }

    // 职位排序功能
    public List<Job> sortJobsByTitle(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getTitle) :
                        Comparator.comparing(Job::getTitle).reversed())
                .collect(Collectors.toList());
    }

    public List<Job> sortJobsByPostDate(boolean ascending) {
        return jobs.stream()
                .sorted(ascending ?
                        Comparator.comparing(Job::getPostDate) :
                        Comparator.comparing(Job::getPostDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Job> sortJobsByDeadline(boolean ascending) {
        return jobs.stream()
                .filter(j -> j.getDeadline() != null)
                .sorted(ascending ?
                        Comparator.comparing(Job::getDeadline) :
                        Comparator.comparing(Job::getDeadline).reversed())
                .collect(Collectors.toList());
    }

    public List<Job> sortJobsByAvailablePositions(boolean ascending) {
        Comparator<Job> comparator = (j1, j2) -> Integer.compare(
                j1.getMaxPositions() - j1.getFilledPositions(),
                j2.getMaxPositions() - j2.getFilledPositions());
        return jobs.stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .collect(Collectors.toList());
    }

    // 职位统计功能
    public Map<String, Integer> getJobStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalJobs", jobs.size());
        stats.put("openJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.OPEN).count());
        stats.put("closedJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.CLOSED).count());
        stats.put("filledJobs", (int) jobs.stream().filter(j -> j.getStatus() == Job.Status.FILLED).count());
        return stats;
    }

    public Map<Job.JobType, Integer> getJobCountByType() {
        Map<Job.JobType, Integer> counts = new HashMap<>();
        for (Job.JobType type : Job.JobType.values()) {
            counts.put(type, (int) jobs.stream().filter(j -> j.getJobType() == type).count());
        }
        return counts;
    }

    public int getTotalAvailablePositions() {
        return jobs.stream()
                .mapToInt(j -> j.getMaxPositions() - j.getFilledPositions())
                .sum();
    }

    public int getTotalFilledPositions() {
        return jobs.stream()
                .mapToInt(Job::getFilledPositions)
                .sum();
    }

    public Map<String, Integer> getJobCountByMO() {
        Map<String, Integer> counts = new HashMap<>();
        jobs.forEach(j -> counts.put(j.getPostedBy(), counts.getOrDefault(j.getPostedBy(), 0) + 1));
        return counts;
    }
}
