package com.banula.navigationservice.controller;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocpi.annotation.AuthorizeHeaders;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.model.OcpiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("${party.api-prefix}/2.2.1/hubclientinfo")
@AllArgsConstructor
@Slf4j
public class HubClientInfoController {

  private final HubClientInfoService hubClientInfoService;

  @GetMapping
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getPaginatedHubClientInfo(
      @RequestParam(value = "date_from", required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
      @RequestParam(value = "date_to", required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
      @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
    
    List<HubClientInfoDTO> hubClientInfos = hubClientInfoService.getPaginatedHubClientInfos(
        dateFrom, dateTo, offset, limit);
    return ResponseEntity.ok(new OcpiResponse<>(hubClientInfos));
  }

  @GetMapping("/{countryCode}/{partyId}")
  @LogRequest
  public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getHubClientInfoByPartyIdAndCountryCode(
      @PathVariable String partyId,
      @PathVariable String countryCode) {
    List<HubClientInfoDTO> hubClientInfo = hubClientInfoService.getHubClientInfoByPartyIdAndCountryCode(partyId, countryCode);
    return ResponseEntity.ok(new OcpiResponse<>(hubClientInfo));
  }

  @PutMapping("/{countryCode}/{partyId}")
  @LogRequest
  public ResponseEntity<OcpiResponse<HubClientInfoDTO>> updateHubClientInfoByPartyIdAndCountryCode(
      @PathVariable String partyId,
      @PathVariable String countryCode,
      @RequestBody HubClientInfoDTO clientInfoDTO) {
    HubClientInfoDTO updatedClientInfo = hubClientInfoService.updateHubClientInfoByPartyIdAndCountryCode(partyId, countryCode, clientInfoDTO);
    return ResponseEntity.ok(new OcpiResponse<>(updatedClientInfo));
  }

}