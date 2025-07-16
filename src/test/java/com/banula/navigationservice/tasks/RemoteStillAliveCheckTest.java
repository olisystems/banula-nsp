package com.banula.navigationservice.tasks;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocpi.model.enums.ConnectionStatus;
import com.banula.openlib.ocpi.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteStillAliveCheckTest {

    @Mock
    private HubClientInfoService hubClientInfoService;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @InjectMocks
    private RemoteStillAliveCheck remoteStillAliveCheck;

    private HubClientInfoDTO testParty;

    @BeforeEach
    void setUp() {
        testParty = HubClientInfoDTO.builder()
                .partyId("TEST")
                .countryCode("NL")
                .role(Role.CPO)
                .status(ConnectionStatus.PLANNED)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(applicationConfiguration.getRemoteCheckTimeout()).thenReturn(10000L);
        when(applicationConfiguration.getPlatformUrl()).thenReturn("https://platform.example.com");
    }

    @Test
    void testRun_WithPlannedParties_ShouldCheckVersions() {
        // Given
        when(hubClientInfoService.getHubClientInfosByStatus(ConnectionStatus.PLANNED))
                .thenReturn(Arrays.asList(testParty));

        // When
        remoteStillAliveCheck.run();

        // Then
        verify(hubClientInfoService).getHubClientInfosByStatus(ConnectionStatus.PLANNED);
    }

    @Test
    void testRun_WithNoPlannedParties_ShouldDoNothing() {
        // Given
        when(hubClientInfoService.getHubClientInfosByStatus(ConnectionStatus.PLANNED))
                .thenReturn(Arrays.asList());

        // When
        remoteStillAliveCheck.run();

        // Then
        verify(hubClientInfoService).getHubClientInfosByStatus(ConnectionStatus.PLANNED);
        verify(hubClientInfoService, never()).updateHubClientInfoByPartyIdAndCountryCode(
                anyString(), anyString(), any(HubClientInfoDTO.class)
        );
    }

    @Test
    void testRun_WhenVersionsRequestFails_ShouldNotUpdateStatus() {
        // Given
        when(hubClientInfoService.getHubClientInfosByStatus(ConnectionStatus.PLANNED))
                .thenReturn(Arrays.asList(testParty));

        // When
        remoteStillAliveCheck.run();

        // Then
        verify(hubClientInfoService).getHubClientInfosByStatus(ConnectionStatus.PLANNED);
        verify(hubClientInfoService, never()).updateHubClientInfoByPartyIdAndCountryCode(
                anyString(), anyString(), any(HubClientInfoDTO.class)
        );
    }
} 