package com.banula.navigationservice.service;

import com.banula.navigationservice.model.MongoClientInfo;
import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.repository.HubClientInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HubClientInfoServiceTest {
//
//  @Mock
//  private HubClientInfoRepository hubClientInfoRepository;
//
//  @InjectMocks
//  private HubClientInfoServiceImpl hubClientInfoService;
//
//  private MongoClientInfo testClientInfo;
//  private HubClientInfoDTO testClientInfoDTO;
//
//  @BeforeEach
//  void setUp() {
//    testClientInfo = MongoClientInfo.builder()
//        .id("test-id")
//        .partyId("TEST")
//        .countryCode("NL")
//        .role("CPO")
//        .status("ACTIVE")
//        .lastUpdated(LocalDateTime.now())
//        .createdAt(LocalDateTime.now())
//        .updatedAt(LocalDateTime.now())
//        .createdBy("test-user")
//        .updatedBy("test-user")
//        .build();
//
//    testClientInfoDTO = HubClientInfoDTO.builder()
//        .id("test-id")
//        .partyId("TEST")
//        .countryCode("NL")
//        .role("CPO")
//        .status("ACTIVE")
//        .lastUpdated(LocalDateTime.now())
//        .createdAt(LocalDateTime.now())
//        .updatedAt(LocalDateTime.now())
//        .createdBy("test-user")
//        .updatedBy("test-user")
//        .build();
//  }
//
//  @Test
//  void getAllHubClientInfos_ShouldReturnAllClientInfos() {
//    // Given
//    List<MongoClientInfo> clientInfos = Arrays.asList(testClientInfo);
//    when(hubClientInfoRepository.findAll()).thenReturn(clientInfos);
//
//    // When
//    List<HubClientInfoDTO> result = hubClientInfoService.getAllHubClientInfos();
//
//    // Then
//    assertNotNull(result);
//    assertEquals(1, result.size());
//    assertEquals(testClientInfo.getPartyId(), result.get(0).getPartyId());
//    verify(hubClientInfoRepository).findAll();
//  }
//
//  @Test
//  void getHubClientInfoById_ShouldReturnClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findById("test-id")).thenReturn(Optional.of(testClientInfo));
//
//    // When
//    HubClientInfoDTO result = hubClientInfoService.getHubClientInfoById("test-id");
//
//    // Then
//    assertNotNull(result);
//    assertEquals(testClientInfo.getPartyId(), result.getPartyId());
//    verify(hubClientInfoRepository).findById("test-id");
//  }
//
//  @Test
//  void getHubClientInfoById_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findById("non-existent")).thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class, () -> hubClientInfoService.getHubClientInfoById("non-existent"));
//    verify(hubClientInfoRepository).findById("non-existent");
//  }
//
//  @Test
//  void getHubClientInfoByPartyIdAndCountryCode_ShouldReturnClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findByPartyIdAndCountryCode("TEST", "NL"))
//        .thenReturn(Optional.of(testClientInfo));
//
//    // When
//    HubClientInfoDTO result = hubClientInfoService.getHubClientInfoByPartyIdAndCountryCode("TEST", "NL");
//
//    // Then
//    assertNotNull(result);
//    assertEquals(testClientInfo.getPartyId(), result.getPartyId());
//    assertEquals(testClientInfo.getCountryCode(), result.getCountryCode());
//    verify(hubClientInfoRepository).findByPartyIdAndCountryCode("TEST", "NL");
//  }
//
//  @Test
//  void getHubClientInfoByPartyIdAndCountryCode_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findByPartyIdAndCountryCode("TEST", "NL"))
//        .thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class,
//        () -> hubClientInfoService.getHubClientInfoByPartyIdAndCountryCode("TEST", "NL"));
//    verify(hubClientInfoRepository).findByPartyIdAndCountryCode("TEST", "NL");
//  }
//
//  @Test
//  void getHubClientInfosByPartyId_ShouldReturnClientInfos() {
//    // Given
//    List<MongoClientInfo> clientInfos = Arrays.asList(testClientInfo);
//    when(hubClientInfoRepository.findByPartyId("TEST")).thenReturn(clientInfos);
//
//    // When
//    List<HubClientInfoDTO> result = hubClientInfoService.getHubClientInfosByPartyId("TEST");
//
//    // Then
//    assertNotNull(result);
//    assertEquals(1, result.size());
//    assertEquals(testClientInfo.getPartyId(), result.get(0).getPartyId());
//    verify(hubClientInfoRepository).findByPartyId("TEST");
//  }
//
//  @Test
//  void getHubClientInfosByCountryCodeAndPartyId_ShouldReturnClientInfos() {
//    // Given
//    List<MongoClientInfo> clientInfos = Arrays.asList(testClientInfo);
//    when(hubClientInfoRepository.findByCountryCodeAndPartyId("NL", "TEST")).thenReturn(clientInfos);
//
//    // When
//    List<HubClientInfoDTO> result = hubClientInfoService.getHubClientInfosByCountryCodeAndPartyId("NL", "TEST");
//
//    // Then
//    assertNotNull(result);
//    assertEquals(1, result.size());
//    assertEquals(testClientInfo.getPartyId(), result.get(0).getPartyId());
//    assertEquals(testClientInfo.getCountryCode(), result.get(0).getCountryCode());
//    verify(hubClientInfoRepository).findByCountryCodeAndPartyId("NL", "TEST");
//  }
//
//  @Test
//  void getHubClientInfosByStatus_ShouldReturnClientInfos() {
//    // Given
//    List<MongoClientInfo> clientInfos = Arrays.asList(testClientInfo);
//    when(hubClientInfoRepository.findByStatus("ACTIVE")).thenReturn(clientInfos);
//
//    // When
//    List<HubClientInfoDTO> result = hubClientInfoService.getHubClientInfosByStatus("ACTIVE");
//
//    // Then
//    assertNotNull(result);
//    assertEquals(1, result.size());
//    assertEquals(testClientInfo.getStatus(), result.get(0).getStatus());
//    verify(hubClientInfoRepository).findByStatus("ACTIVE");
//  }
//
//  @Test
//  void createHubClientInfo_ShouldCreateAndReturnClientInfo() {
//    // Given
//    when(hubClientInfoRepository.save(any(MongoClientInfo.class))).thenReturn(testClientInfo);
//
//    // When
//    HubClientInfoDTO result = hubClientInfoService.createHubClientInfo(testClientInfoDTO);
//
//    // Then
//    assertNotNull(result);
//    assertEquals(testClientInfo.getPartyId(), result.getPartyId());
//    verify(hubClientInfoRepository).save(any(MongoClientInfo.class));
//  }
//
//  @Test
//  void updateHubClientInfo_ShouldUpdateAndReturnClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findById("test-id")).thenReturn(Optional.of(testClientInfo));
//    when(hubClientInfoRepository.save(any(MongoClientInfo.class))).thenReturn(testClientInfo);
//
//    // When
//    HubClientInfoDTO result = hubClientInfoService.updateHubClientInfo("test-id", testClientInfoDTO);
//
//    // Then
//    assertNotNull(result);
//    assertEquals(testClientInfo.getPartyId(), result.getPartyId());
//    verify(hubClientInfoRepository).findById("test-id");
//    verify(hubClientInfoRepository).save(any(MongoClientInfo.class));
//  }
//
//  @Test
//  void updateHubClientInfo_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findById("non-existent")).thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class,
//        () -> hubClientInfoService.updateHubClientInfo("non-existent", testClientInfoDTO));
//    verify(hubClientInfoRepository).findById("non-existent");
//    verify(hubClientInfoRepository, never()).save(any());
//  }
//
//  @Test
//  void updateHubClientInfoStatus_ShouldUpdateStatusAndReturnClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findById("test-id")).thenReturn(Optional.of(testClientInfo));
//    when(hubClientInfoRepository.save(any(MongoClientInfo.class))).thenReturn(testClientInfo);
//
//    // When
//    HubClientInfoDTO result = hubClientInfoService.updateHubClientInfoStatus("test-id", "INACTIVE");
//
//    // Then
//    assertNotNull(result);
//    verify(hubClientInfoRepository).findById("test-id");
//    verify(hubClientInfoRepository).save(any(MongoClientInfo.class));
//  }
//
//  @Test
//  void updateHubClientInfoStatus_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findById("non-existent")).thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class,
//        () -> hubClientInfoService.updateHubClientInfoStatus("non-existent", "INACTIVE"));
//    verify(hubClientInfoRepository).findById("non-existent");
//    verify(hubClientInfoRepository, never()).save(any());
//  }
//
//  @Test
//  void deleteHubClientInfo_ShouldDeleteClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findById("test-id")).thenReturn(Optional.of(testClientInfo));
//    doNothing().when(hubClientInfoRepository).deleteById("test-id");
//
//    // When
//    hubClientInfoService.deleteHubClientInfo("test-id");
//
//    // Then
//    verify(hubClientInfoRepository).findById("test-id");
//    verify(hubClientInfoRepository).deleteById("test-id");
//  }
//
//  @Test
//  void deleteHubClientInfo_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findById("non-existent")).thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class, () -> hubClientInfoService.deleteHubClientInfo("non-existent"));
//    verify(hubClientInfoRepository).findById("non-existent");
//    verify(hubClientInfoRepository, never()).deleteById(any());
//  }
//
//  @Test
//  void deleteHubClientInfoByPartyIdAndCountryCode_ShouldDeleteClientInfo() {
//    // Given
//    when(hubClientInfoRepository.findByPartyIdAndCountryCode("TEST", "NL"))
//        .thenReturn(Optional.of(testClientInfo));
//    doNothing().when(hubClientInfoRepository).delete(testClientInfo);
//
//    // When
//    hubClientInfoService.deleteHubClientInfoByPartyIdAndCountryCode("TEST", "NL");
//
//    // Then
//    verify(hubClientInfoRepository).findByPartyIdAndCountryCode("TEST", "NL");
//    verify(hubClientInfoRepository).delete(testClientInfo);
//  }
//
//  @Test
//  void deleteHubClientInfoByPartyIdAndCountryCode_ShouldThrowException_WhenNotFound() {
//    // Given
//    when(hubClientInfoRepository.findByPartyIdAndCountryCode("TEST", "NL"))
//        .thenReturn(Optional.empty());
//
//    // When & Then
//    assertThrows(RuntimeException.class,
//        () -> hubClientInfoService.deleteHubClientInfoByPartyIdAndCountryCode("TEST", "NL"));
//    verify(hubClientInfoRepository).findByPartyIdAndCountryCode("TEST", "NL");
//    verify(hubClientInfoRepository, never()).delete(any());
//  }
}