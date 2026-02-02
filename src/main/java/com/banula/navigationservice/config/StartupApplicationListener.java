package com.banula.navigationservice.config;

import com.banula.navigationservice.service.HubClientInfoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.banula.openlib.ocpi.platform.PlatformClient;
import com.banula.openlib.ocpi.util.InfoUtils;

@Component
@AllArgsConstructor
@Slf4j
public class StartupApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    private final ApplicationConfiguration applicationConfiguration;
    private final PlatformClient platformClient;
    private final HubClientInfoService hubClientInfoService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.setProperty("java.awt.headless", "true");
        log.info("Changed default time zone to  {} ", TimeZone.getDefault().getDisplayName());
        log.info("Open library version: {}", InfoUtils.getLibVersion("com.my-oli", "banula-open-library"));

        log.info("My OCPI URL: {}/api/v1/internal/ocpi/2.2.1 | port: {}", applicationConfiguration.getPartyUrl(),
                event.getApplicationContext().getEnvironment().getProperty("server.port"));
        log.info("My Non-OCPI URL: {}/api/v1 | port: {}", applicationConfiguration.getPartyUrl(),
                event.getApplicationContext().getEnvironment().getProperty("server.port"));

        log.info("Sync of hubclientinfo from OCN Node started...");
        hubClientInfoService.syncAllHubClientInfoParties();
        log.info("Completed sync of hubclientinfo");
    }
}
