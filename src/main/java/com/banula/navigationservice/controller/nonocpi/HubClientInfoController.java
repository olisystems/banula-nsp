package com.banula.navigationservice.controller.nonocpi;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.model.OcpiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("${party.api-non-ocpi-prefix}/clientinfo")
@AllArgsConstructor
@Slf4j
public class HubClientInfoController {

  private final HubClientInfoService hubClientInfoService;

  /**
   * Get all client infos
   * 
   * @return List of all client infos
   */
  @GetMapping
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getAllClientInfos() {
    List<HubClientInfoDTO> clientInfos = hubClientInfoService.getAllHubClientInfos();
    return ResponseEntity.ok(new OcpiResponse<>(clientInfos));
  }

  /**
   * Get client info by ID
   * 
   * @param id The client info ID
   * @return Client info
   */
  @GetMapping("/{id}")
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> getClientInfoById(@PathVariable String id) {
    HubClientInfoDTO clientInfo = hubClientInfoService.getHubClientInfoById(id);
    return ResponseEntity.ok(new OcpiResponse<>(clientInfo));
  }

  /**
   * Get client info by party ID and country code
   * 
   * @param partyId     Party ID
   * @param countryCode Country code
   * @return Client info
   */
  @GetMapping("/party/{partyId}/country/{countryCode}")
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> getClientInfoByPartyIdAndCountryCode(
      @PathVariable String partyId,
      @PathVariable String countryCode) {
    HubClientInfoDTO clientInfo = hubClientInfoService.getHubClientInfoByPartyIdAndCountryCode(partyId, countryCode);
    return ResponseEntity.ok(new OcpiResponse<>(clientInfo));
  }

  /**
   * Get client infos by party ID
   * 
   * @param partyId Party ID
   * @return List of client infos
   */
  @GetMapping("/party/{partyId}")
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getClientInfosByPartyId(
      @PathVariable String partyId) {
    List<HubClientInfoDTO> clientInfos = hubClientInfoService.getHubClientInfosByPartyId(partyId);
    return ResponseEntity.ok(new OcpiResponse<>(clientInfos));
  }

  /**
   * Get client infos by country code and party ID
   * 
   * @param countryCode Country code
   * @param partyId     Party ID
   * @return List of client infos
   */
  @GetMapping("/country/{countryCode}/party/{partyId}")
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getClientInfosByCountryCodeAndPartyId(
      @PathVariable String countryCode,
      @PathVariable String partyId) {
    List<HubClientInfoDTO> clientInfos = hubClientInfoService.getHubClientInfosByCountryCodeAndPartyId(countryCode,
        partyId);
    return ResponseEntity.ok(new OcpiResponse<>(clientInfos));
  }

  /**
   * Get client infos by status
   * 
   * @param status Status
   * @return List of client infos
   */
  @GetMapping("/status/{status}")
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getClientInfosByStatus(@PathVariable String status) {
    List<HubClientInfoDTO> clientInfos = hubClientInfoService.getHubClientInfosByStatus(status);
    return ResponseEntity.ok(new OcpiResponse<>(clientInfos));
  }

  /**
   * Create a new client info
   * 
   * @param clientInfoDTO Client info data
   * @return Created client info
   */
  @PostMapping
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> createClientInfo(
      @RequestBody @Valid HubClientInfoDTO clientInfoDTO) {
    HubClientInfoDTO createdClientInfo = hubClientInfoService.createHubClientInfo(clientInfoDTO);
    return ResponseEntity.ok(new OcpiResponse<>(createdClientInfo));
  }

  /**
   * Update client info by ID
   * 
   * @param id            Client info ID
   * @param clientInfoDTO Updated client info data
   * @return Updated client info
   */
  @PutMapping("/{id}")
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> updateClientInfo(
      @PathVariable String id,
      @RequestBody @Valid HubClientInfoDTO clientInfoDTO) {
    HubClientInfoDTO updatedClientInfo = hubClientInfoService.updateHubClientInfo(id, clientInfoDTO);
    return ResponseEntity.ok(new OcpiResponse<>(updatedClientInfo));
  }

  /**
   * Update client info status
   * 
   * @param id     Client info ID
   * @param status New status
   * @return Updated client info
   */
  @PatchMapping("/{id}/status")
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> updateClientInfoStatus(
      @PathVariable String id,
      @RequestParam String status) {
    HubClientInfoDTO updatedClientInfo = hubClientInfoService.updateHubClientInfoStatus(id, status);
    return ResponseEntity.ok(new OcpiResponse<>(updatedClientInfo));
  }

  /**
   * Delete client info by ID
   * 
   * @param id Client info ID
   * @return Success message
   */
  @DeleteMapping("/{id}")
  @LogRequest
  public ResponseEntity<OcpiResponse<String>> deleteClientInfo(@PathVariable String id) {
    hubClientInfoService.deleteHubClientInfo(id);
    return ResponseEntity.ok(new OcpiResponse<>("Client info deleted successfully"));
  }

  /**
   * Delete client info by party ID and country code
   * 
   * @param partyId     Party ID
   * @param countryCode Country code
   * @return Success message
   */
  @DeleteMapping("/party/{partyId}/country/{countryCode}")
  @LogRequest
  public ResponseEntity<OcpiResponse<String>> deleteClientInfoByPartyIdAndCountryCode(
      @PathVariable String partyId,
      @PathVariable String countryCode) {
    hubClientInfoService.deleteHubClientInfoByPartyIdAndCountryCode(partyId, countryCode);
    return ResponseEntity.ok(new OcpiResponse<>("Client info deleted successfully"));
  }
}