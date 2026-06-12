package com.banula.navigationservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.openlib.ocpi.model.ClientInfo;
import com.banula.openlib.ocpi.model.dto.ClientInfoDTO;
import com.banula.openlib.ocpi.model.enums.InterfaceRole;
import com.banula.openlib.ocpi.model.enums.ModuleID;
import com.banula.openlib.ocpi.platform.PlatformClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class NSPNotificationServiceImpl implements NSPNotificationService {

    private final ApplicationConfiguration applicationConfiguration;
    private final PlatformClient platformClient;

    @Override
    public void broadcastHubClientInfoUpdate(ClientInfo clientInfo) {
        try {
            String tenantId = applicationConfiguration.getPlatformCountryCode() + "_" + applicationConfiguration.getPlatformPartyId();
            ClientInfoDTO clientInfoDTO = new ClientInfoDTO(clientInfo.getPartyId(), clientInfo.getCountryCode(),
                    clientInfo.getRole(), clientInfo.getStatus(), clientInfo.getLastUpdated());
            platformClient.sendOutflowRequest(
                    tenantId,
                    clientInfo.getCountryCode(),
                    clientInfo.getPartyId(),
                    InterfaceRole.SENDER,
                    ModuleID.HUB_CLIENT_INFO,
                    HttpMethod.PUT,
                    clientInfoDTO,
                    new ParameterizedTypeReference<>() {},
                    List.of(),
                    Map.of());
        } catch (Exception e) {
            log.debug("Error broadcasting client info party update {} ({}): {}",
                    clientInfo.getPartyId(), clientInfo.getCountryCode(), e.getMessage());
        }
    }
}
