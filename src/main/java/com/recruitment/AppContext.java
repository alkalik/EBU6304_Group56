package com.recruitment;

import com.recruitment.service.ApplicationService;
import com.recruitment.service.JobService;
import com.recruitment.service.NotificationService;
import com.recruitment.service.UserService;

public class AppContext {
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

    public AppContext() {
        this.userService = new UserService();
        this.jobService = new JobService();
        this.applicationService = new ApplicationService();
        this.notificationService = new NotificationService(userService);

        this.jobService.setApplicationService(applicationService);
        this.jobService.setNotificationService(notificationService);
        this.applicationService.setJobService(jobService);
        this.applicationService.setNotificationService(notificationService);
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

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
