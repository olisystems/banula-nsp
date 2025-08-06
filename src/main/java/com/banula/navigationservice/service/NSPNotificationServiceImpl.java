package com.banula.navigationservice.service;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.openlib.ocn.client.OcnClient;
import com.banula.openlib.ocpi.model.ClientInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class NSPNotificationServiceImpl implements NSPNotificationService {

    private final ApplicationConfiguration applicationConfiguration;
    private final OcnClient ocnClient;

    @Override
    public void broadcastHubClientInfoUpdate(ClientInfo clientInfo) {
        try {
            String outflowUrl = applicationConfiguration.getPlatformUrl() + "/ocpi/outflow/ocpi/2.2/hubclientinfo";
            ocnClient.executeOcpiOperation(
                    outflowUrl,
                    clientInfo,
                    "",
                    "",
                    new ParameterizedTypeReference<>() {
                    },
                    HttpMethod.PUT,
                    List.of());
        } catch (Exception e) {
            log.debug("Error broadcasting client info party update {} ({}): {}",
                    clientInfo.getPartyId(), clientInfo.getCountryCode(), e.getMessage());
        }
    }
}
