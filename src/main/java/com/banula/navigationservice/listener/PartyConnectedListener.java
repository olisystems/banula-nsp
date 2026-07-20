package com.banula.navigationservice.listener;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.event.PartyConnectedEvent;
import com.banula.navigationservice.service.LocationSyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PartyConnectedListener {

    private final LocationSyncService locationSyncService;
    private final ApplicationConfiguration applicationConfiguration;

    @Async
    @EventListener
    public void onPartyConnected(PartyConnectedEvent event) {
        if (!Boolean.TRUE.equals(applicationConfiguration.getLocationSyncEnabled())) {
            return;
        }
        try {
            locationSyncService.welcomeParty(event.getParty());
        } catch (Exception e) {
            log.warn("Welcome ceremony failed for {}/{}: {}", event.getParty().getCountryCode(),
                    event.getParty().getPartyId(), e.getMessage(), e);
        }
    }
}
