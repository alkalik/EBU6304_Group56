package com.recruitment;

import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.service.ServiceTestBase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AppContextTest extends ServiceTestBase {
    @Test
    public void contextWiresApplicationPipelineDependencies() {
        AppContext context = new AppContext();

        Job job = new Job();
        job.setTitle("Context Wiring Job");
        job.setPostedBy("MO-context");
        job.setJobType(Job.JobType.MODULE_TA);
        job.setMaxPositions(1);

        Job created = context.getJobService().createJob(job);
        Application application = context.getApplicationService()
                .apply(created.getId(), "USR-context-ta", "Testing context wiring.");

        assertNotNull(application);
        assertNotNull(context.getNotificationService());
    }
}
