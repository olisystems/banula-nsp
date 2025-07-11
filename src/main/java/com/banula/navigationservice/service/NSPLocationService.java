package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.model.vo.Connector;
import com.banula.openlib.ocpi.model.vo.EVSE;

public interface NSPLocationService {
    Object getLocationConnector(String countryCode, String party_id, String locationId, String evseUid,
            String connectorId);

    void putLocation(LocationDTO locationDTO, String countryCode, String partyId, String ocpiId);

    void putEvse(EVSE evseVO, String countryCode, String party_id, String locationId, String evseUid);

    void putConnector(Connector connectorVO, String countryCode, String party_id, String locationId, String evseUid,
            String connectorId);

    void patchLocation(LocationDTO locationDTO, String countryCode, String partyId, String locationId);

    void patchEvse(EVSE evse, String countryCode, String party_id, String locationId, String evseUid);

    void patchConnector(Connector connector, String countryCode, String party_id, String locationId, String evseUid,
            String connectorId);

}
