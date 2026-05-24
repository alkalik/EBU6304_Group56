package com.recruitment.service;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public abstract class ServiceTestBase {
    private static final Path DATA_DIR = Paths.get("data");
    private Path backupDir;

    @Before
    public void preserveRuntimeData() throws IOException {
        backupDir = Files.createTempDirectory("recruitment-test-data-");
        if (Files.exists(DATA_DIR)) {
            copyDirectory(DATA_DIR, backupDir.resolve("data"));
        }
        resetDataDirectory();
    }

    @After
    public void restoreRuntimeData() throws IOException {
        resetDataDirectory();
        Path savedData = backupDir.resolve("data");
        if (Files.exists(savedData)) {
            copyDirectory(savedData, DATA_DIR);
        }
        deleteDirectory(backupDir);
    }

    protected ServiceGraph newServiceGraph() {
        UserService userService = new UserService();
        JobService jobService = new JobService();
        ApplicationService applicationService = new ApplicationService();
        NotificationService notificationService = new NotificationService(userService);

        jobService.setApplicationService(applicationService);
        jobService.setNotificationService(notificationService);
        applicationService.setJobService(jobService);
        applicationService.setNotificationService(notificationService);

        return new ServiceGraph(userService, jobService, applicationService, notificationService);
    }

    private void resetDataDirectory() throws IOException {
        if (Files.exists(DATA_DIR)) {
            deleteDirectory(DATA_DIR);
        }
        Files.createDirectories(DATA_DIR);
        Files.write(DATA_DIR.resolve("config.properties"), Arrays.asList(
                "deepseek.enabled=false",
                "deepseek.api.key=",
                "deepseek.api.url=https://api.deepseek.com/v1/chat/completions",
                "deepseek.model=deepseek-chat"
        ));
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .map(Path::toFile)
                .sorted((a, b) -> b.getAbsolutePath().compareTo(a.getAbsolutePath()))
                .forEach(File::delete);
    }

    protected static class ServiceGraph {
        protected final UserService userService;
        protected final JobService jobService;
        protected final ApplicationService applicationService;
        protected final NotificationService notificationService;

        private ServiceGraph(UserService userService, JobService jobService,
                             ApplicationService applicationService,
                             NotificationService notificationService) {
            this.userService = userService;
            this.jobService = jobService;
            this.applicationService = applicationService;
            this.notificationService = notificationService;
        }
    }
}
