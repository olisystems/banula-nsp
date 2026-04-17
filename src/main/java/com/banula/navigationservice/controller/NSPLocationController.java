package com.banula.navigationservice.controller;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.service.NSPLocationService;
import com.banula.openlib.ocpi.annotation.LogRequest;
import com.banula.openlib.ocpi.annotation.PlatformRequest;
import com.banula.openlib.ocpi.annotation.ValidateConnector;
import com.banula.openlib.ocpi.annotation.ValidateEVSE;
import com.banula.openlib.ocpi.annotation.ValidateLocation;
import com.banula.openlib.ocpi.model.OcpiResponse;
import com.banula.openlib.ocpi.model.PlatformRequestValues;
import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.model.vo.Connector;
import com.banula.openlib.ocpi.model.vo.EVSE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/ocpi/2.2.1/locations")
@AllArgsConstructor
@Slf4j
public class NSPLocationController {

    protected final NSPLocationService locationService;
    protected final ApplicationConfiguration applicationConfiguration;

    // sender OCPI Location interface endpoints
    /**
     * Fetch a list of Locations, last updated between the {date_from} and {date_to}
     * (paginated)
     * 
     * @param dateFrom Only return Locations that have last_updated after or equal
     *                 to this Date/Time (inclusive).
     * @param dateTo   Only return Locations that have last_updated before or equal
     *                 to this Date/Time (inclusive).
     * @param offset   The offset of the first object returned. Default is 0.
     * @param limit    Maximum number of objects to GET.
     * @return List of all Locations with valid EVSEs.
     */
    @GetMapping
    // used only for ocpi complient endpoints, this is the only one here, other
    // methods should use AuthorizeTokenB
    @LogRequest
    public ResponseEntity<OcpiResponse<List<LocationDTO>>> getLocations(
            @RequestParam(value = "date_from", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "date_to", required = false) LocalDateTime dateTo,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return ResponseEntity.ok(new OcpiResponse<>(
                locationService.findLocations(dateFrom, dateTo, offset, limit)));
    }

    /**
     * Retrieve a Location as it is stored in the eMSP system.
     * 
     * @param countryCode Country code of the CPO requesting data from the eMSP
     *                    system.
     * @param party_id    Party ID (Provider ID) of the CPO requesting data from the
     *                    eMSP system.
     * @param locationId  Location.id of the Location object to retrieve.
     * @param evseUid     Evse.uid, required when requesting an EVSE or Connector
     *                    object.
     * @param connectorId Evse.uid, required when requesting an EVSE or Connector
     *                    object.
     * @return The response contains the requested object:
     *         Location - If a Location object was requested: the Location object.
     *         EVSE - If an EVSE object was requested: the EVSE object.
     *         Connector - If a Connector object was requested: the Connector
     *         object.
     */
    @GetMapping(value = { "/{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}",
            "/{countryCode}/{partyId}/{locationId}/{evseUid}", "/{countryCode}/{partyId}/{locationId}" })
    public ResponseEntity<OcpiResponse<Object>> getLocationEvseConnector(
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @PathVariable(value = "evseUid", required = false) String evseUid,
            @PathVariable(value = "connectorId", required = false) String connectorId) {
        return ResponseEntity.ok(new OcpiResponse<>(locationService.getLocationEvseConnector(
                countryCode, party_id, locationId, evseUid, connectorId)));
    }

    /**
     * The CPO pushes available Location, EVSE or Connector objects to the eMSP. PUT
     * can be used to send
     * new Location objects to the eMSP but also to replace existing Locations.
     * 
     * @param locationDTO The request body contains the new/updated object.
     * @param countryCode Country code, required.
     * @param partyId     Party ID (Provider ID), required.
     * @param locationId  Location.id, required.
     */

    @ValidateLocation
    @PutMapping(value = { "/{countryCode}/{partyId}/{locationId}" })
    @LogRequest
    public ResponseEntity<OcpiResponse<String>> putLocation(
            @RequestBody @Valid LocationDTO locationDTO,
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String partyId,
            @PathVariable(value = "locationId") String locationId) {
        locationDTO.setPublish(false);
        locationService.putLocation(locationDTO, countryCode, partyId, locationId);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

    @LogRequest
    @ValidateEVSE
    @PutMapping(value = { "/{countryCode}/{partyId}/{locationId}/{evseUid}" })
    public ResponseEntity<OcpiResponse<String>> putLocationEvse(
            @RequestBody @Valid EVSE evseVO,
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @PathVariable(value = "evseUid", required = false) String evseUid) {
        // push the updated locationDTO to the database
        locationService.putEvse(evseVO, countryCode, party_id, locationId, evseUid);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

    @PutMapping(value = { "/{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}" })
    @ValidateConnector
    @LogRequest
    public ResponseEntity<OcpiResponse<String>> putLocationEvseConnector(
            @RequestBody @Valid Connector connectorVO,
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @PathVariable(value = "evseUid") String evseUid,
            @PathVariable(value = "connectorId") String connectorId) {
        // push the updated locationDTO to the database
        locationService.putConnector(connectorVO, countryCode, party_id, locationId, evseUid, connectorId);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

    @Operation(summary = "Partially update a location", description = "Updates only the specified fields of a location. Unspecified fields remain unchanged.\n\n"
            +
            "Example URL: http://localhost:8085/api/v1/internal/ocpi/2.2.1/locations/DE/ABC/ARCMIND1", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Minimal PATCH Example", value = """
                    {
                        "name": "Mukunds second lab"
                    }
                    """))))
    @PatchMapping(value = { "/{countryCode}/{partyId}/{locationId}" })
    @LogRequest
    public ResponseEntity<OcpiResponse<String>> patchLocation(
            @RequestBody LocationDTO locationDTO,

            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId) {
        locationService.patchLocation(locationDTO, countryCode, party_id, locationId);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

    @ValidateEVSE
    @PatchMapping(value = { "/{countryCode}/{partyId}/{locationId}/{evseUid}" })
    @LogRequest
    public ResponseEntity<OcpiResponse<String>> patchEVSE(
            @RequestBody EVSE evse,
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @PathVariable(value = "evseUid") String evseUid) {
        locationService.patchEvse(evse, countryCode, party_id, locationId, evseUid);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

    @LogRequest
    @PatchMapping(value = { "/{countryCode}/{partyId}/{locationId}/{evseUid}/{connectorId}" })
    public ResponseEntity<OcpiResponse<String>> patchLocationEvseConnector(
            @RequestBody Connector connectorVO,
            @PathVariable(value = "countryCode") String countryCode,
            @PathVariable(value = "partyId") String party_id,
            @PathVariable(value = "locationId") String locationId,
            @PathVariable(value = "evseUid") String evseUid,
            @PathVariable(value = "connectorId") String connectorId) {
        locationService.patchConnector(connectorVO, countryCode, party_id, locationId, evseUid, connectorId);
        return ResponseEntity.ok(new OcpiResponse<>(null));
    }

}
