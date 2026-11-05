package com.banula.navigationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpMethod;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.config.MongoCollectionMapper;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.repository.HubClientInfoRepository;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.exception.OcpiUpstreamException;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.enums.InterfaceRole;
import com.banula.openlib.ocpi.model.enums.ModuleID;
import com.banula.openlib.ocpi.platform.PlatformClient;

@ExtendWith(MockitoExtension.class)
class HubClientInfoServiceImplTest {

    @Mock
    private HubClientInfoRepository hubClientInfoRepository;

    @Mock
    private PlatformClient platformClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private MongoCollectionMapper mongoCollectionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private HubClientInfoServiceImpl hubClientInfoService;

    @BeforeEach
    void setUp() {
        when(applicationConfiguration.getPlatformCountryCode()).thenReturn("DE");
        when(applicationConfiguration.getPlatformPartyId()).thenReturn("HUB");
        when(applicationConfiguration.getPlatformTenantId()).thenReturn("DE_NSP");
    }

    @Test
    void pullHubClientInfoFromHub_shouldKeepTheHubStatusCode() {
        whenTheHubIsAsked().thenThrow(new OcpiUpstreamException(
                "GET hubclientinfo to DE/HUB failed with OCPI status 2001: Missing parameter", 2001, 200));

        OCPICustomException ex = assertThrows(OCPICustomException.class,
                () -> hubClientInfoService.pullHubClientInfoFromHub());

        assertEquals(2001, ex.getStatusCode());
        assertTrue(ex.getErrorMessage().startsWith("The hub rejected the HubClientInfo request"));
        assertTrue(ex.getErrorMessage().contains("Missing parameter"));
    }

    @Test
    void pullHubClientInfoFromHub_shouldReportAnUnreachableHub() {
        whenTheHubIsAsked().thenThrow(new OCPICustomException(
                "GET hubclientinfo to DE/HUB failed: Connection refused"));

        OCPICustomException ex = assertThrows(OCPICustomException.class,
                () -> hubClientInfoService.pullHubClientInfoFromHub());

        assertEquals(3000, ex.getStatusCode());
        assertTrue(ex.getErrorMessage().startsWith("Could not reach the hub to sync HubClientInfo"));
    }

    private OngoingStubbing<OcpiResponse<List<HubClientInfoDTO>>> whenTheHubIsAsked() {
        return when(platformClient.sendOutflowRequest(eq("DE_NSP"), eq("DE"), eq("HUB"), eq(InterfaceRole.SENDER),
                eq(ModuleID.HUB_CLIENT_INFO), eq(HttpMethod.GET), isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<OcpiResponse<List<HubClientInfoDTO>>>>any(),
                anyList(), anyMap()));
    }
}
