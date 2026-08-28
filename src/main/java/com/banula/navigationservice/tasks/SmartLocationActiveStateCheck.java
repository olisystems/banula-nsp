package com.banula.navigationservice.tasks;

import org.springframework.stereotype.Component;

import com.banula.navigationservice.service.NSPSmartLocationService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily job that re-evaluates every VERIFIED/ACTIVE smart location against its
 * activation window, promoting the ones whose window covers today and demoting
 * the ones whose window has passed.
 */
@Slf4j
@Component
@AllArgsConstructor
public class SmartLocationActiveStateCheck implements Runnable {

    private final NSPSmartLocationService nspSmartLocationService;

    @Override
    public void run() {
        log.info("Starting smart location active state check");
        int changed = nspSmartLocationService.refreshActiveStates();
        log.info("Smart location active state check completed, {} location(s) changed state", changed);
    }

}
