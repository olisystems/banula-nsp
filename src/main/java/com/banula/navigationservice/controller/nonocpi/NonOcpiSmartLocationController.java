package com.banula.navigationservice.controller.nonocpi;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.dto.BulkImportResultDTO;
import com.banula.navigationservice.service.NSPSmartLocationService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocationState;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.model.OcpiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @GetMapping("/party-set")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<Set<String>>> getPartySet() {
        return ResponseEntity.ok(new OcpiResponse<>(nspSmartLocationService.getPartySet()));
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

    @GetMapping("/by-malo/{maloId}")
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
        smartLocationDTO.setSmartLocationState(SmartLocationState.ENRICHED);
        SmartLocationDTO updatedLocation = nspSmartLocationService.patchSmartLocation(countryCode, party_id, locationId,
                smartLocationDTO);

        if (updatedLocation == null) {
            String locationKey = countryCode + "*" + party_id + "*" + locationId;
            return ResponseEntity.status(404).body(
                    new OcpiResponse<>(null, 2003, "Location " + locationKey + " not found"));
        }

        return ResponseEntity.ok(new OcpiResponse<>(updatedLocation));
    }

    @Operation(summary = "Partially update a smart location", description = "Updates specific fields of a smart location identified by country code, party ID, and location ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location successfully updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OcpiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Location not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OcpiResponse.class)))
    })
    @PatchMapping("/{countryCode}/{partyId}/{locationId}")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> patchSmartLocation(
            @Parameter(description = "Country code", example = "DE") @PathVariable(value = "countryCode") String countryCode,
            @Parameter(description = "Party ID", example = "ABC") @PathVariable(value = "partyId") String partyId,
            @Parameter(description = "Location ID", example = "ARCMIND1") @PathVariable(value = "locationId") String locationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Smart location data to update", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmartLocationDTO.class), examples = @ExampleObject(name = "Update default supplier", value = "{\n  \"default_supplier\": {\n    \"default_supplier_malo_id\": \"DEF_SUP_1234\"\n  }\n}"))) @RequestBody SmartLocationDTO smartLocationDTO,
            HttpServletRequest request) {
        SmartLocationDTO updatedLocation = nspSmartLocationService.patchSmartLocation(countryCode, partyId, locationId,
                smartLocationDTO);

        if (updatedLocation == null) {
            String locationKey = countryCode + "*" + partyId + "*" + locationId;
            return ResponseEntity.status(404).body(
                    new OcpiResponse<>(null, 2003, "Location " + locationKey + " not found"));
        }

        return ResponseEntity.ok(new OcpiResponse<>(updatedLocation));
    }

    @Operation(summary = "Bulk import smart locations from CSV", description = "Enriches existing locations using a CSV file. Each row is patched independently; rows that fail are reported in the response.")
    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<BulkImportResultDTO>> bulkImport(
            @RequestParam("file") MultipartFile file) {
        BulkImportResultDTO result = nspSmartLocationService.bulkImport(file);
        return ResponseEntity.ok(new OcpiResponse<>(result));
    }

    @Operation(summary = "Download smart location import template", description = "Returns a CSV with the import header row and one row per existing location, with country_code, party_id and location_id pre-filled. Optionally filter by countryCode and partyId.")
    @Parameters({
            @Parameter(name = "countryCode", in = ParameterIn.QUERY, required = false, description = "Filter by country code", schema = @Schema(type = "string")),
            @Parameter(name = "partyId", in = ParameterIn.QUERY, required = false, description = "Filter by party ID", schema = @Schema(type = "string"))
    })
    @GetMapping("/bulk-import/template")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<byte[]> downloadImportTemplate(
            @org.springframework.web.bind.annotation.RequestParam(value = "countryCode", required = false) String countryCode,
            @org.springframework.web.bind.annotation.RequestParam(value = "partyId", required = false) String partyId) {
        String csv = nspSmartLocationService.generateImportTemplate(countryCode, partyId);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        String filename = (!isBlankParam(countryCode) && !isBlankParam(partyId))
                ? "smart-locations-template-" + countryCode + "-" + partyId + ".csv"
                : "smart-locations-template.csv";
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static boolean isBlankParam(String value) {
        return value == null || value.trim().isEmpty();
    }
}
