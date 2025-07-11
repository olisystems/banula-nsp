package com.banula.navigationservice.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;

import org.jetbrains.annotations.NotNull;
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

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        System.setProperty("java.awt.headless", "true");
        log.info("Changed default time zone to  {} ", TimeZone.getDefault().getDisplayName());
        log.info("Open library version: {}", InfoUtils.getLibVersion("com.my-oli", "banula-open-library"));
        try {
            platformClient.updateOcnVersionDetailsFromPlatform();
        } catch (Exception ex) {
            log.error(String.format("OCN party registration error: %s", ex.getLocalizedMessage()));
            for (StackTraceElement ste : ex.getStackTrace()) {
                System.out.println(ste);
            }
        }
        log.info("OCN version details retrieved from Platform: {} endpoints",
                applicationConfiguration.getOcnVersionDetails().getEndpoints().size());
        log.info("My OCPI URL: {}{} | port: {}", applicationConfiguration.getPartyUrl(),
                applicationConfiguration.getApiPrefix(),
                event.getApplicationContext().getEnvironment().getProperty("server.port"));
        log.info("My Non-OCPI URL: {}{} | port: {}", applicationConfiguration.getPartyUrl(),
                applicationConfiguration.getApiNonOcpiPrefix(),
                event.getApplicationContext().getEnvironment().getProperty("server.port"));
        try {
        } catch (Exception ex) {
            log.error(String.format("OCN party registration error: %s", ex.getLocalizedMessage()));
            for (StackTraceElement ste : ex.getStackTrace()) {
                System.out.println(ste);
            }
        }
    }
}
