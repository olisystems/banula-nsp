package com.banula.navigationservice.service;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;

import java.time.LocalDateTime;

public interface LocationSyncService {

    /** Pull locations from a newly connected CPO and publish them through the hub. */
    void welcomeParty(HubClientInfoDTO party);

    /** Hourly sync: pull recent location updates from connected CPOs and broadcast via the hub. */
    void syncRecentLocations();

    void pullStoreAndBroadcast(String countryCode, String partyId, LocalDateTime dateFrom, LocalDateTime dateTo);
}
