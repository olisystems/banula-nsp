package com.banula.navigationservice.service;

import java.time.LocalDateTime;
import java.util.List;

import com.banula.openlib.ocpi.model.dto.ConnectorDTO;
import com.banula.openlib.ocpi.model.dto.EvseDTO;
import com.banula.openlib.ocpi.model.dto.LocationDTO;

public interface NSPLocationService {
        Object getLocationEvseConnector(String countryCode, String partyId, String locationId, String evseUid,
                        String connectorId);

        void putLocation(LocationDTO locationDTO, String countryCode, String partyId, String ocpiId);

        void putEvse(EvseDTO evseVO, String countryCode, String partyId, String locationId, String evseUid);

        void putConnector(ConnectorDTO connectorVO, String countryCode, String partyId, String locationId,
                        String evseUid,
                        String connectorId);

        void patchLocation(LocationDTO locationDTO, String countryCode, String partyId, String locationId);

        void patchEvse(EvseDTO evse, String countryCode, String partyId, String locationId, String evseUid);

        void patchConnector(ConnectorDTO connector, String countryCode, String partyId, String locationId,
                        String evseUid,
                        String connectorId);

        List<LocationDTO> findLocations(LocalDateTime dateFrom, LocalDateTime dateTo, Integer offset, Integer limit);

}
