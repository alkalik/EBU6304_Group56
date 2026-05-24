package com.recruitment;

import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.UserService;

/**
 * Lightweight dependency container holding the core service layer instances.
 * <p>
 * Created once per application session (or frame hierarchy) so UI components
 * share the same in-memory repositories backed by JSON persistence.
 */
public class AppContext {
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    /**
     * Constructs a new context with default service implementations.
     */
    public AppContext() {
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
    }

    /**
     * @return shared user authentication and profile service
     */
    public UserService getUserService() {
        return userService;
    }

    /**
     * @return shared job posting and vacancy service
     */
    public JobService getJobService() {
        return jobService;
    }

    /**
     * @return shared job application workflow service
     */
    public ApplicationService getApplicationService() {
        return applicationService;
    }
}
