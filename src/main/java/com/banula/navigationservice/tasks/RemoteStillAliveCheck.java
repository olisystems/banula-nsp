package com.banula.navigationservice.tasks;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocn.client.OcnClient;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.dto.response.VersionResponseDTO;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class RemoteStillAliveCheck implements Runnable {

    private final HubClientInfoService hubClientInfoService;
    private final OcnClient ocnClient;
    private final ApplicationConfiguration applicationConfiguration;

    @Override
    public void run() {
        log.info("Starting remote still alive check for parties with PLANNED status");

        try {
            // Get all parties with PLANNED status
            List<HubClientInfoDTO> plannedParties = hubClientInfoService.getHubClientInfosByStatus(ConnectionStatus.PLANNED);

            if (plannedParties.isEmpty()) {
                log.info("No parties with PLANNED status found");
                return;
            }

            log.info("Found {} parties with PLANNED status, checking their versions endpoint", plannedParties.size());

            for (HubClientInfoDTO party : plannedParties) {
                checkPartyVersions(party);
            }

            log.info("Still alive check completed");

        } catch (Exception e) {
            log.error("Error during remote still alive check: {}", e.getMessage(), e);
        }
    }
    
    private void checkPartyVersions(HubClientInfoDTO party) {
        try {
            // Construct the outflow URL for versions endpoint
            String outflowUrl = applicationConfiguration.getPlatformUrl() + "/ocpi/outflow/ocpi/2.2/versions";
            
            log.debug("Checking versions endpoint for party {} ({}): {}", 
                     party.getPartyId(), party.getCountryCode(), outflowUrl);
            
            // Request the versions endpoint using OCN client
            CompletableFuture<OcpiResponse<List<VersionResponseDTO>>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return ocnClient.executeOcpiOperation(
                            outflowUrl,
                            null,
                            party.getPartyId(),
                            party.getCountryCode(),
                            new ParameterizedTypeReference<>() {
                            },
                            HttpMethod.GET,
                            List.of()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            OcpiResponse<List<VersionResponseDTO>> response = future.get(
                applicationConfiguration.getRemoteCheckTimeout(), 
                TimeUnit.MILLISECONDS
            );
            
            // If we get a successful response (status_code 1000), update the party status
            if (response != null &&
                    response.getStatus_code() == 1000 &&
                    response.getData() != null &&
                    !response.getData().isEmpty()
            ) {

                log.info("Party {} ({}) is now online, updating status from PLANNED to CONNECTED",
                        party.getPartyId(), party.getCountryCode());
                
                // Create updated party info with current timestamp
                HubClientInfoDTO updatedParty = HubClientInfoDTO.builder()
                        .partyId(party.getPartyId())
                        .countryCode(party.getCountryCode())
                        .role(party.getRole())
                        .status(ConnectionStatus.CONNECTED) // Keep current status for now
                        .lastUpdated(java.time.LocalDateTime.now())
                        .build();
                
                hubClientInfoService.updateHubClientInfoByPartyIdAndCountryCode(
                    party.getPartyId(), 
                    party.getCountryCode(), 
                    updatedParty
                );
            } else {
                log.debug("Party {} ({}) is still offline",
                        party.getPartyId(), party.getCountryCode());
            }
            
        } catch (Exception e) {
            log.debug("Party {} ({}) is still offline: {}", 
                     party.getPartyId(), party.getCountryCode(), e.getMessage());
            // Party is still offline, no action needed
        }
    }
    

}
