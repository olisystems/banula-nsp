package com.banula.navigationservice.service;

import com.banula.navigationservice.dto.BulkImportResultDTO;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;

import org.springframework.web.multipart.MultipartFile;

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

        BulkImportResultDTO bulkImport(MultipartFile file);

        String generateImportTemplate(String countryCode, String partyId);

        /**
         * Re-evaluates every VERIFIED/ACTIVE smart location against today's date in
         * the configured zone, promoting those whose activation window covers today
         * and demoting those whose window has passed. Idempotent: a second run in the
         * same day changes nothing.
         *
         * @return the number of locations whose state actually changed
         */
        int refreshActiveStates();
}
