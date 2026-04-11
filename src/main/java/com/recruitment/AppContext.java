package com.recruitment;

import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.UserService;

public class AppContext {
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public AppContext() {
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
    }

    public UserService getUserService() {
        return userService;
    }

    public JobService getJobService() {
        return jobService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }
}
