package com.banula.navigationservice.config;

import com.banula.navigationservice.tasks.RemoteStillAliveCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@AllArgsConstructor
public class SchedulerConfig {

    private final RemoteStillAliveCheck remoteStillAliveCheck;
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Scheduled task to check if offline parties are now connected
     * Runs based on configured interval (default: 5 minutes)
     */
    @Scheduled(fixedRateString = "${remote-check.interval:300000}")
    public void scheduleRemoteStillAliveCheck() {
        if (!applicationConfiguration.getRemoteCheckEnabled()) {
            log.debug("Remote still alive check is disabled");
            return;
        }
        
        log.debug("Executing scheduled remote still alive check");
        try {
            remoteStillAliveCheck.run();
        } catch (Exception e) {
            log.error("Error executing scheduled remote still alive check: {}", e.getMessage(), e);
        }
    }
} 