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

import jakarta.servlet.http.HttpServletRequest;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.service.NSPSmartLocationService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.model.OcpiResponse;

//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/internal/locations")
// @Tag(name="CpoNonOcpiLocation")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class NonOcpiSmartLocationController {
    protected final NSPSmartLocationService nspSmartLocationService;
    protected final ApplicationConfiguration applicationConfiguration;

    @GetMapping
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<List<SmartLocationDTO>>> getAllLocations() {
        return ResponseEntity.ok(new OcpiResponse<>(nspSmartLocationService.getAllLocations()));
    }

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

    @PostMapping("/{countryCode}/{partyId}/{locationId}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> saveSmartLocation(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @RequestBody SmartLocationDTO smartLocationDTO,
            HttpServletRequest request) {
        SmartLocationDTO updatedLocation = nspSmartLocationService.saveSmartLocation(locationId, countryCode, party_id,
                smartLocationDTO);

        if (updatedLocation == null) {
            String locationKey = countryCode + "*" + party_id + "*" + locationId;
            return ResponseEntity.status(404).body(
                    new OcpiResponse<>(null, 2003, "Location " + locationKey + " not found"));
        }

        return ResponseEntity.ok(new OcpiResponse<>(updatedLocation));
    }
}
