package com.banula.navigationservice.controller.nonocpi;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.http.ContentDisposition;
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

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.dto.BulkImportResultDTO;
import com.banula.navigationservice.service.LocationSyncService;
import com.banula.navigationservice.service.NSPSmartLocationService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.annotation.OcpiGetCompositeId;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocationState;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.util.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
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
    protected final ObjectMapper objectMapper;
    protected final LocationSyncService locationSyncService;

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
    @OcpiGetCompositeId
    public ResponseEntity<OcpiResponse<SmartLocationDTO>> getLocation(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId) {
        SmartLocationDTO smartLocation = nspSmartLocationService.getLocation(countryCode, party_id, locationId);
        if (smartLocation == null) {
            String locationKey = countryCode + "*" + party_id + "*" + locationId;
            return ResponseEntity.status(404).body(
                    new OcpiResponse<>(null, 2003, "Location " + locationKey + " not found"));
        }
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

    @Operation(summary = "Partially update a smart location", description = "Updates specific fields of a smart location identified by country code, party ID, and location ID. "
            + "Only the fields present in the body are touched. The activation window needs active_first_day only: leaving active_last_day out opens it ended. "
            + "Sending a day explicitly as null clears it, which is how a mistakenly entered active_last_day is removed — the location then leaves ARCHIVED and becomes ACTIVE again "
            + "when its first day is today or earlier. Sending smart_location_state without any window day still clears both days. "
            + "active_last_day must not be before active_first_day, and cannot be set without one. "
            + "ACTIVE and ARCHIVED are rejected: both are derived from the window, never set by hand.")
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
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Smart location data to update", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmartLocationDTO.class), examples = @ExampleObject(name = "Clear the last active day", value = "{\n  \"active_last_day\": null\n}"))) @RequestBody JsonNode requestBody,
            HttpServletRequest request) {
        // Bound as a JsonNode so an absent key ("leave unchanged") stays
        // distinguishable from an explicit null ("clear this day"); the DTO alone
        // collapses both into null.
        SmartLocationDTO smartLocationDTO = objectMapper.convertValue(requestBody, SmartLocationDTO.class);

        SmartLocationDTO updatedLocation = nspSmartLocationService.patchSmartLocation(countryCode, partyId, locationId,
                smartLocationDTO, isExplicitNull(requestBody, "active_first_day"),
                isExplicitNull(requestBody, "active_last_day"));

        if (updatedLocation == null) {
            String locationKey = countryCode + "*" + partyId + "*" + locationId;
            return ResponseEntity.status(404).body(
                    new OcpiResponse<>(null, 2003, "Location " + locationKey + " not found"));
        }

        return ResponseEntity.ok(new OcpiResponse<>(updatedLocation));
    }

    @Operation(summary = "Re-evaluate smart location active states", description = "Runs the same evaluation as the daily 00:00:05 job in the configured api.zone-id time zone: a location whose activation window covers today becomes ACTIVE, one whose window has passed becomes ARCHIVED, and one whose window has not started yet waits as VERIFIED. Idempotent — running it twice in the same day changes nothing. Returns the number of locations whose state actually changed.")
    @PostMapping("/refresh-active-states")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<Integer>> refreshActiveStates() {
        return ResponseEntity.ok(new OcpiResponse<>(nspSmartLocationService.refreshActiveStates()));
    }

    @Operation(summary = "Pull locations from CPOs now", description = "Runs the same work as the hourly location sync, on demand. Given countryCode and partyId it pulls that one CPO regardless of its connection status, which is what makes it usable right after a location is created. Given neither, it syncs every CONNECTED CPO exactly as the scheduled job does. The window ends now and starts lookbackHours earlier, defaulting to location-sync.lookback-hours. Returns the number of locations stored.")
    @Parameters({
            @Parameter(name = "countryCode", in = ParameterIn.QUERY, required = false, description = "Country code of the CPO to pull from; must be paired with partyId", schema = @Schema(type = "string")),
            @Parameter(name = "partyId", in = ParameterIn.QUERY, required = false, description = "Party ID of the CPO to pull from; must be paired with countryCode", schema = @Schema(type = "string")),
            @Parameter(name = "lookbackHours", in = ParameterIn.QUERY, required = false, description = "How many hours back to pull; defaults to location-sync.lookback-hours", schema = @Schema(type = "integer"))
    })
    @PostMapping("/pull-locations")
    @LogRequest
    @CrossOrigin
    public ResponseEntity<OcpiResponse<Integer>> pullLocations(
            @RequestParam(value = "countryCode", required = false) String countryCode,
            @RequestParam(value = "partyId", required = false) String partyId,
            @RequestParam(value = "lookbackHours", required = false) Long lookbackHours) {

        boolean hasCountryCode = !isBlankParam(countryCode);
        boolean hasPartyId = !isBlankParam(partyId);

        if (hasCountryCode != hasPartyId) {
            return ResponseEntity.badRequest().body(new OcpiResponse<>(null,
                    Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS,
                    "countryCode and partyId must be provided together"));
        }

        if (!hasCountryCode) {
            return ResponseEntity.ok(new OcpiResponse<>(locationSyncService.syncRecentLocations()));
        }

        long hours = lookbackHours != null ? lookbackHours : applicationConfiguration.getLocationSyncLookbackHours();
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime from = to.minusHours(hours);
        return ResponseEntity.ok(new OcpiResponse<>(
                locationSyncService.pullStoreAndBroadcast(countryCode, partyId, from, to)));
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
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static boolean isBlankParam(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Whether the caller explicitly sent {@code "field": null} — as opposed to
     * leaving the key out, which means "leave this field unchanged".
     */
    private static boolean isExplicitNull(JsonNode requestBody, String field) {
        return requestBody != null && requestBody.has(field) && requestBody.get(field).isNull();
    }

}
