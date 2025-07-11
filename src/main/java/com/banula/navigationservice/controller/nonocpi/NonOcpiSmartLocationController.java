package com.banula.navigationservice.controller.nonocpi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.service.NSPSmartLocationService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.model.OcpiResponse;

//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("${party.api-non-ocpi-prefix}/locations")
// @Tag(name="CpoNonOcpiLocation")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class NonOcpiSmartLocationController {
    protected final NSPSmartLocationService nspSmartLocationService;
    protected final ApplicationConfiguration applicationConfiguration;

    @GetMapping("/{countryCode}/{partyId}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<List<SmartLocationDTO>>> getLocationsForParty(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id) {
        return ResponseEntity.ok(new OcpiResponse<>(nspSmartLocationService.getLocationsByParty(
                countryCode, party_id)));
    }

    @GetMapping("/{maloId}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> getLocationsByMaloId(
            @PathVariable(value = "maloId") String maloId) {
        return ResponseEntity.ok(new OcpiResponse<>(nspSmartLocationService.getLocationByMaloId(maloId)));
    }

    @GetMapping("/{countryCode}/{partyId}/{locationId}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> getLocation(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId) {
        SmartLocationDTO smartLocation = nspSmartLocationService.getLocation(countryCode, party_id, locationId);
        return ResponseEntity.ok(new OcpiResponse<>(smartLocation));
    }

    @PostMapping("/{countryCode}/{partyId}/{id}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> saveSmartLocation(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "id") String id,
            @RequestBody SmartLocationDTO smartLocationDTO) {
        nspSmartLocationService.saveSmartLocation(id, countryCode, party_id, smartLocationDTO);
        return ResponseEntity.ok(new OcpiResponse<>(smartLocationDTO));
    }
}
