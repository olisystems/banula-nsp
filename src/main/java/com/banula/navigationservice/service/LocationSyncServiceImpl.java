package com.banula.navigationservice.service;

import com.banula.navigationservice.client.NspPlatformClient;
import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import com.banula.openlib.ocpi.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class LocationSyncServiceImpl implements LocationSyncService {

    private final NspPlatformClient nspPlatformClient;
    private final NSPLocationService locationService;
    private final HubClientInfoService hubClientInfoService;
    private final ApplicationConfiguration applicationConfiguration;

    @Override
    public void welcomeParty(HubClientInfoDTO party) {
        if (party == null || party.getRole() != Role.CPO) {
            return;
        }
        if (party.getStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        if (isSelf(party.getCountryCode(), party.getPartyId())) {
            return;
        }

        log.info("Welcome ceremony: pulling locations from CPO {}/{}", party.getCountryCode(), party.getPartyId());
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime from = to.minusDays(applicationConfiguration.getLocationSyncWelcomeLookbackDays());
        pullStoreAndBroadcast(party.getCountryCode(), party.getPartyId(), from, to);
    }

    @Override
    public int syncRecentLocations() {
        return syncRecentLocations(applicationConfiguration.getLocationSyncLookbackHours());
    }

    @Override
    public int syncRecentLocations(long lookbackHours) {
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime from = to.minusHours(lookbackHours);

        List<HubClientInfoDTO> connected = hubClientInfoService
                .getHubClientInfosByStatus(List.of(ConnectionStatus.CONNECTED));

        List<HubClientInfoDTO> cpos = connected.stream()
                .filter(p -> p.getRole() == Role.CPO)
                .filter(p -> !isSelf(p.getCountryCode(), p.getPartyId()))
                .toList();

        log.info("Hourly location sync: {} connected CPO(s), window {} -> {}", cpos.size(), from, to);
        int stored = 0;
        for (HubClientInfoDTO cpo : cpos) {
            try {
                stored += pullStoreAndBroadcast(cpo.getCountryCode(), cpo.getPartyId(), from, to);
            } catch (Exception e) {
                log.warn("Location sync failed for {}/{}: {}", cpo.getCountryCode(), cpo.getPartyId(), e.getMessage());
            }
        }
        return stored;
    }

    @Override
    public int pullStoreAndBroadcast(String countryCode, String partyId, LocalDateTime dateFrom,
            LocalDateTime dateTo) {
        if (applicationConfiguration.getPlatformTenantId() == null) {
            log.warn("Skipping location sync for {}/{}: platform hub identity is not configured", countryCode,
                    partyId);
            return 0;
        }

        List<LocationDTO> locations = nspPlatformClient.getLocations(countryCode, partyId, dateFrom, dateTo);
        if (locations == null || locations.isEmpty()) {
            log.info("No locations returned from {}/{} for window {} -> {}", countryCode, partyId, dateFrom, dateTo);
            return 0;
        }

        log.info("Pulled {} location(s) from {}/{}; storing locally then PUT to hub for OCN broadcast",
                locations.size(), countryCode, partyId);
        int stored = 0;
        for (LocationDTO location : locations) {
            ensureOwner(location, countryCode, partyId);
            if (!sameParty(location.getCountryCode(), location.getPartyId(), countryCode, partyId)) {
                log.warn("Ignoring location {} with unexpected owner {}/{} from {}/{}",
                        location.getId(), location.getCountryCode(), location.getPartyId(), countryCode, partyId);
                continue;
            }
            try {
                locationService.putLocation(location, location.getCountryCode(), location.getPartyId(),
                        location.getId());
                stored++;
            } catch (Exception e) {
                log.warn("Failed to store location {} from {}/{}: {}", location.getId(), countryCode, partyId,
                        e.getMessage());
                continue;
            }
            try {
                // OCPI-to = hub (DE/BAN) → node broadcasts via ModuleNotificationService
                nspPlatformClient.putLocationToHub(location);
            } catch (Exception e) {
                log.warn("Failed to put location {} to hub from {}/{}: {}", location.getId(), countryCode, partyId,
                        e.getMessage());
            }
        }

        log.info("Finished pull/store/hub-put for {} of {} location(s) from {}/{}", stored, locations.size(),
                countryCode, partyId);
        return stored;
    }

    private void ensureOwner(LocationDTO location, String countryCode, String partyId) {
        if (location.getCountryCode() == null || location.getCountryCode().isBlank()) {
            location.setCountryCode(countryCode);
        }
        if (location.getPartyId() == null || location.getPartyId().isBlank()) {
            location.setPartyId(partyId);
        }
    }

    private boolean isSelf(String countryCode, String partyId) {
        return sameParty(countryCode, partyId, applicationConfiguration.getPlatformCountryCode(),
                applicationConfiguration.getPlatformPartyId());
    }

    private boolean sameParty(String countryA, String partyA, String countryB, String partyB) {
        return Objects.equals(normalize(countryA), normalize(countryB))
                && Objects.equals(normalize(partyA), normalize(partyB));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
