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

        /**
         * Same as
         * {@link #patchSmartLocation(String, String, String, SmartLocationDTO)}, but
         * able to clear an activation window day: the DTO collapses "key absent" and
         * "explicit null" into the same null, so the controller passes the distinction
         * in separately.
         *
         * @param clearActiveFirstDay the caller sent {@code "active_first_day": null}
         * @param clearActiveLastDay  the caller sent {@code "active_last_day": null}
         */
        SmartLocationDTO patchSmartLocation(String countryCode, String partyId, String id,
                        SmartLocationDTO smartLocationDTO, boolean clearActiveFirstDay,
                        boolean clearActiveLastDay);

        BulkImportResultDTO bulkImport(MultipartFile file);

        String generateImportTemplate(String countryCode, String partyId);

        /**
         * Re-evaluates every VERIFIED/ACTIVE/ARCHIVED smart location against today's
         * date in the configured zone: a window covering today activates the
         * location, a window that has passed archives it, and a window still in the
         * future leaves it VERIFIED. Idempotent: a second run in the same day changes
         * nothing.
         *
         * @return the number of locations whose state actually changed
         */
        int refreshActiveStates();
}
