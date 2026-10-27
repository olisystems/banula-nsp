package com.banula.navigationservice.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.navigationservice.service.NSPNotificationService;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.exception.OcpiUpstreamException;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.dto.response.VersionResponseDTO;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import com.banula.openlib.ocpi.model.enums.InterfaceRole;
import com.banula.openlib.ocpi.model.enums.ModuleID;
import com.banula.openlib.ocpi.model.enums.Role;
import com.banula.openlib.ocpi.model.enums.VersionNumber;
import com.banula.openlib.ocpi.platform.PlatformClient;

/**
 * PlatformClient used to return OCPI error answers; it now throws them. The check must still mark
 * a party that answers with an error as offline, and leave it alone when the platform itself fails.
 */
@ExtendWith(MockitoExtension.class)
class RemoteStillAliveCheckTest {

    @Mock
    private HubClientInfoService hubClientInfoService;

    @Mock
    private PlatformClient platformClient;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private NSPNotificationService nspNotificationService;

    @InjectMocks
    private RemoteStillAliveCheck remoteStillAliveCheck;

    @BeforeEach
    void setUp() {
        HubClientInfoDTO party = HubClientInfoDTO.builder()
                .countryCode("NL")
                .partyId("EMS")
                .role(Role.EMSP)
                .status(ConnectionStatus.CONNECTED)
                .build();
        when(hubClientInfoService.getHubClientInfosByStatus(anyList())).thenReturn(List.of(party));
        when(applicationConfiguration.getPlatformUrl()).thenReturn("http://platform");
        when(applicationConfiguration.getPlatformTenantId()).thenReturn("DE_NSP");
        when(applicationConfiguration.getRemoteCheckTimeout()).thenReturn(5000L);
    }

    @Test
    void run_shouldMarkPartyOfflineWhenItAnswersWithAnOcpiError() {
        whenVersionsAreRequested().thenThrow(new OcpiUpstreamException(
                "GET versions to NL/EMS failed with OCPI status 3001: Unable to use the client's API", 3001, 200));

        remoteStillAliveCheck.run();

        assertEquals(ConnectionStatus.OFFLINE, updatedStatus());
        verify(nspNotificationService).broadcastHubClientInfoUpdate(any());
    }

    @Test
    void run_shouldMarkPartyOfflineWhenItAnswersWithoutABody() {
        whenVersionsAreRequested().thenThrow(new OcpiUpstreamException(
                "GET versions to NL/EMS failed: empty response", 3000, 200));

        remoteStillAliveCheck.run();

        assertEquals(ConnectionStatus.OFFLINE, updatedStatus());
    }

    @Test
    void run_shouldLeavePartyAloneWhenThePlatformAnswersWithAnHttpError() {
        whenVersionsAreRequested().thenThrow(new OcpiUpstreamException(
                "GET versions to NL/EMS failed with HTTP 502", 3000, 502));

        remoteStillAliveCheck.run();

        verifyPartyNotUpdated();
    }

    @Test
    void run_shouldLeavePartyAloneWhenThePlatformCannotBeReached() {
        whenVersionsAreRequested().thenThrow(new OCPICustomException(
                "GET versions to NL/EMS failed: Connection refused"));

        remoteStillAliveCheck.run();

        verifyPartyNotUpdated();
    }

    @Test
    void run_shouldKeepPartyConnectedWhenItAnswersWithVersions() {
        whenVersionsAreRequested().thenReturn(new OcpiResponse<>(
                List.of(new VersionResponseDTO(VersionNumber.V_2_2_1, "https://emsp/ocpi/2.2.1"))));

        remoteStillAliveCheck.run();

        assertEquals(ConnectionStatus.CONNECTED, updatedStatus());
        verify(nspNotificationService, never()).broadcastHubClientInfoUpdate(any());
    }

    private OngoingStubbing<OcpiResponse<List<VersionResponseDTO>>> whenVersionsAreRequested() {
        return when(platformClient.sendOutflowRequest(eq("DE_NSP"), eq("NL"), eq("EMS"), eq(InterfaceRole.SENDER),
                eq(ModuleID.VERSIONS), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<OcpiResponse<List<VersionResponseDTO>>>>any(),
                anyList(), anyMap()));
    }

    private ConnectionStatus updatedStatus() {
        ArgumentCaptor<HubClientInfoDTO> captor = ArgumentCaptor.forClass(HubClientInfoDTO.class);
        verify(hubClientInfoService).updateHubClientInfoByPartyIdAndCountryCode(eq("EMS"), eq("NL"),
                captor.capture());
        return captor.getValue().getStatus();
    }

    private void verifyPartyNotUpdated() {
        verify(hubClientInfoService, never()).updateHubClientInfoByPartyIdAndCountryCode(anyString(), anyString(),
                any());
        verify(nspNotificationService, never()).broadcastHubClientInfoUpdate(any());
    }
}
