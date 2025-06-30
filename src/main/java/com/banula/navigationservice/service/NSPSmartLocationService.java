package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;

import java.util.List;

public interface NSPSmartLocationService {
    List<SmartLocationDTO> getLocationsByParty(String countryCode, String partyId);

    SmartLocationDTO getLocation(String countryCode, String partyId, String locationId);

    SmartLocationDTO getLocationByMaloId(String maloId);

    void saveSmartLocation(String id, String countryCode, String partyId, SmartLocationDTO smartLocationDTO);
}
