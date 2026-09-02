package com.banula.navigationservice.controller.nonocpi;

import com.banula.navigationservice.model.dto.HubClientInfoDTO;
import com.banula.navigationservice.service.HubClientInfoService;
import com.banula.openlib.ocpi.annotation.AuthorizeHeaders;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.model.OcpiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/hubclientinfo")
// @Tag(name="CpoNonOcpiLocation")
@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class NonOcpiHubClientInfoController {

    private final HubClientInfoService hubClientInfoService;

    @Operation(summary = "List hub client info parties", description = "Paginated view of every known party and its current connection status (PLANNED, CONNECTED, OFFLINE). Without a date window the whole registry is returned.")
    @Parameters({
            @Parameter(name = "dateFrom", in = ParameterIn.QUERY, required = false, description = "Only parties last updated at or after this instant (ISO-8601)", schema = @Schema(type = "string", format = "date-time")),
            @Parameter(name = "dateTo", in = ParameterIn.QUERY, required = false, description = "Only parties last updated at or before this instant (ISO-8601)", schema = @Schema(type = "string", format = "date-time")),
            @Parameter(name = "offset", in = ParameterIn.QUERY, required = false, description = "Records to skip; defaults to 0", schema = @Schema(type = "integer")),
            @Parameter(name = "limit", in = ParameterIn.QUERY, required = false, description = "Maximum records to return; defaults to 20", schema = @Schema(type = "integer"))
    })
    @GetMapping
    @LogRequest
    public ResponseEntity<OcpiResponse<List<HubClientInfoDTO>>> getHubClientInfos(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {

        // Both nulls means "no date criteria": the underlying query filters on
        // createdAt, which these documents do not carry, so any window matches nothing.
        return ResponseEntity.ok(new OcpiResponse<>(
                hubClientInfoService.getPaginatedHubClientInfos(dateFrom, dateTo, offset, limit)));
    }

    @Operation(summary = "Sync hub client info from the hub", description = "Pulls the party list from the hub over OCPI and applies every record, reconciling statuses that went stale locally. Returns how many parties were applied.")
    @PostMapping("/sync")
    @LogRequest
    public ResponseEntity<OcpiResponse<String>> syncHubClientInfos() {
        int synced = hubClientInfoService.pullHubClientInfoFromHub();
        return ResponseEntity.ok(new OcpiResponse<>(
                synced + (synced == 1 ? " party" : " parties") + " synced from the hub"));
    }

    @PutMapping("/{countryCode}/{partyId}")
    @LogRequest
    public ResponseEntity<OcpiResponse<HubClientInfoDTO>> updateHubClientInfoByPartyIdAndCountryCode(
            @PathVariable String partyId,
            @PathVariable String countryCode,
            @RequestBody HubClientInfoDTO clientInfoDTO) {
        HubClientInfoDTO updatedClientInfo = hubClientInfoService.updateHubClientInfoByPartyIdAndCountryCode(partyId,
                countryCode, clientInfoDTO);
        return ResponseEntity.ok(new OcpiResponse<>(updatedClientInfo));
    }
}