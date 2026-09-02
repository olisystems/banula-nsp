package com.banula.navigationservice.service;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;

import java.time.LocalDateTime;

public interface LocationSyncService {

    /** Pull locations from a newly connected CPO and publish them through the hub. */
    void welcomeParty(HubClientInfoDTO party);

    /**
     * Hourly sync: pull recent location updates from connected CPOs and broadcast via the hub.
     *
     * @return how many locations were stored across all connected CPOs
     */
    int syncRecentLocations();

    /**
     * Pull one party's locations for a window, store them and publish through the hub.
     * Does not consult the party's connection status, so it can be driven on demand.
     *
     * @return how many locations were stored
     */
    int pullStoreAndBroadcast(String countryCode, String partyId, LocalDateTime dateFrom, LocalDateTime dateTo);
}
