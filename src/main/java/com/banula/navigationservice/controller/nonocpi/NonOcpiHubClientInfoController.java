package com.banula.navigationservice.controller.nonocpi;


import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocpi.annotation.AuthorizeHeaders;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.model.OcpiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${party.api-non-ocpi-prefix}/hubclientinfo")
// @Tag(name="CpoNonOcpiLocation")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class NonOcpiHubClientInfoController {

    private final HubClientInfoService hubClientInfoService;

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