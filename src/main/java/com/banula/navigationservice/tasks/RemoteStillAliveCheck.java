package com.banula.navigationservice.tasks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.mapper.ClientInfoMapper;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.navigationservice.service.NSPNotificationService;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.dto.response.VersionResponseDTO;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import com.banula.openlib.ocpi.model.enums.InterfaceRole;
import com.banula.openlib.ocpi.model.enums.ModuleID;
import com.banula.openlib.ocpi.platform.PlatformClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class RemoteStillAliveCheck implements Runnable {

    private final HubClientInfoService hubClientInfoService;
    private final PlatformClient platformClient;
    private final ApplicationConfiguration applicationConfiguration;
    private final NSPNotificationService nspNotificationService;

    @Override
    public void run() {
        log.info("Starting remote still alive check");

        try {
            // OFFLINE is included so a party that dropped can come back: leaving it
            // out made the transition one-way and no party ever recovered.
            List<HubClientInfoDTO> parties = dedupeKeepingNewest(hubClientInfoService
                    .getHubClientInfosByStatus(
                            List.of(ConnectionStatus.PLANNED, ConnectionStatus.CONNECTED, ConnectionStatus.OFFLINE)));

            if (parties.isEmpty()) {
                log.info("No parties to check");
                return;
            }

            log.info("Found {} parties, checking their versions endpoint", parties.size());

            for (HubClientInfoDTO party : parties) {
                checkPartyVersions(party);
            }

            log.info("Still alive check completed");

        } catch (Exception e) {
            log.error("Error during remote still alive check: {}", e.getMessage(), e);
        }
    }

    /**
     * One entry per party/role, keeping the most recently updated document.
     *
     * <p>
     * The collection carries no unique index, so a write race can leave several documents for the
     * same key (see {@code HubClientInfoServiceImpl#findExistingClientInfo}). Without this the same
     * party would be probed and broadcast once per duplicate. The results arrive sorted by
     * lastUpdated descending, so the first occurrence of a key is the newest one. Role stays part
     * of the key: a party may legitimately be registered as both CPO and eMSP, and the update path
     * is keyed by role as well, so those records must each be checked.
     */
    private static List<HubClientInfoDTO> dedupeKeepingNewest(List<HubClientInfoDTO> parties) {
        Map<String, HubClientInfoDTO> newestByKey = new LinkedHashMap<>();
        for (HubClientInfoDTO party : parties) {
            String key = party.getCountryCode() + "*" + party.getPartyId() + "*" + party.getRole();
            newestByKey.putIfAbsent(key, party);
        }
        return new ArrayList<>(newestByKey.values());
    }

    private void checkPartyVersions(HubClientInfoDTO party) {
        try {
            // Construct the outflow URL for versions endpoint
            String outflowUrl = applicationConfiguration.getPlatformUrl() + "/ocpi/outflow/ocpi/2.2.1/versions";

            log.debug("Checking versions endpoint for party {} ({}): {}",
                    party.getPartyId(), party.getCountryCode(), outflowUrl);

            // Request the versions endpoint using Platform client
            CompletableFuture<OcpiResponse<List<VersionResponseDTO>>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return platformClient.sendOutflowRequest(
                            applicationConfiguration.getPlatformTenantId(),
                            party.getCountryCode(),
                            party.getPartyId(),
                            InterfaceRole.SENDER,
                            ModuleID.VERSIONS,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<OcpiResponse<List<VersionResponseDTO>>>() {
                            },
                            List.of(),
                            Map.of());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            OcpiResponse<List<VersionResponseDTO>> response = future.get(
                    applicationConfiguration.getRemoteCheckTimeout(),
                    TimeUnit.MILLISECONDS);

            HubClientInfoDTO.HubClientInfoDTOBuilder updatedPartyBuilder = HubClientInfoDTO.builder()
                    .partyId(party.getPartyId())
                    .countryCode(party.getCountryCode())
                    .role(party.getRole())
                    .lastUpdated(LocalDateTime.now(ZoneOffset.UTC));


            // If we get a successful response (status_code 1000), update the party status
            if (response != null &&
                    response.getStatus_code() == 1000 &&
                    response.getData() != null &&
                    !response.getData().isEmpty()) {

                log.info("Party {} ({}) is now online, updating status from PLANNED to CONNECTED",
                        party.getPartyId(), party.getCountryCode());

                updatedPartyBuilder.status(ConnectionStatus.CONNECTED);
            } else {
                log.debug("Party {} ({}) is offline",
                        party.getPartyId(), party.getCountryCode());
                updatedPartyBuilder.status(ConnectionStatus.OFFLINE);
            }

            HubClientInfoDTO updatedParty = updatedPartyBuilder.build();
            hubClientInfoService.updateHubClientInfoByPartyIdAndCountryCode(
                    party.getPartyId(),
                    party.getCountryCode(),
                    updatedParty);

            if(party.getStatus() != updatedParty.getStatus()) {
                nspNotificationService.broadcastHubClientInfoUpdate(
                        ClientInfoMapper.toMongoClientInfo(updatedParty)
                );
            }

        } catch (Exception e) {
            log.debug("Error trying to update party {} ({}): {}",
                    party.getPartyId(), party.getCountryCode(), e.getMessage());
        }
    }

}
