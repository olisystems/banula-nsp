package com.banula.navigationservice.service;

import org.springframework.stereotype.Service;

import com.banula.openlib.ocpi.model.ClientInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NSPNotificationServiceImpl implements NSPNotificationService {

    @Override
    public void broadcastHubClientInfoUpdate(ClientInfo clientInfo) {
        // NSP only accepts incoming requests and does not broadcast updates
        log.debug("Broadcast hub client info update called for {} ({}): skipped - NSP is receive-only",
                clientInfo.getPartyId(), clientInfo.getCountryCode());
    }
}
