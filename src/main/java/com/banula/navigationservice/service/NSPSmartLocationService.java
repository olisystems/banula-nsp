package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;

import java.util.List;
import java.util.Set;

public interface NSPSmartLocationService {
        List<SmartLocationDTO> getLocationsByParty(String countryCode, String partyId);

        SmartLocationDTO getLocation(String countryCode, String partyId, String locationId);

        SmartLocationDTO getLocationByMaloId(String maloId);

        List<SmartLocationDTO> getAllLocations();

        Set<String> getPartySet();

        SmartLocationDTO patchSmartLocation(String countryCode, String partyId, String id,
                        SmartLocationDTO smartLocationDTO);

}
