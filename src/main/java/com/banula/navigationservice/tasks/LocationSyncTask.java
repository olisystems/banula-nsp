package com.banula.navigationservice.tasks;

import com.banula.navigationservice.service.LocationSyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class LocationSyncTask implements Runnable {

    private final LocationSyncService locationSyncService;

    @Override
    public void run() {
        log.info("Starting scheduled location sync");
        try {
            locationSyncService.syncRecentLocations();
            log.info("Scheduled location sync completed");
        } catch (Exception e) {
            log.error("Scheduled location sync failed: {}", e.getMessage(), e);
        }
    }
}
