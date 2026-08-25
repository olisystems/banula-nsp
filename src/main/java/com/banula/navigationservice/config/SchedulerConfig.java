package com.banula.navigationservice.config;

import com.banula.navigationservice.tasks.RemoteStillAliveCheck;
import com.banula.navigationservice.tasks.SmartLocationActiveStateCheck;
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
    private final SmartLocationActiveStateCheck smartLocationActiveStateCheck;
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

    /**
     * Scheduled task that flips smart locations between VERIFIED and ACTIVE
     * depending on their activation window.
     *
     * <p>
     * Runs at 00:00:05 in the configured zone (Europe/Berlin), i.e. right after the
     * calendar day the window is expressed in has rolled over. DST transitions
     * happen at 02:00/03:00 local, so this trigger is never skipped or repeated.
     */
    @Scheduled(cron = "${active-state-check.cron:5 0 0 * * *}", zone = "${api.zone-id}")
    public void scheduleSmartLocationActiveStateCheck() {
        if (!applicationConfiguration.getActiveStateCheckEnabled()) {
            log.debug("Smart location active state check is disabled");
            return;
        }

        log.debug("Executing scheduled smart location active state check");
        try {
            smartLocationActiveStateCheck.run();
        } catch (Exception e) {
            log.error("Error executing scheduled smart location active state check: {}", e.getMessage(), e);
        }
    }
}
