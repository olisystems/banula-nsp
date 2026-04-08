package com.banula.navigationservice.service;

import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.navigationservice.util.LocationUtility;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.mapper.LocationMapper;
import com.banula.openlib.ocpi.model.Location;
import com.banula.openlib.ocpi.model.dto.LocationDTO;
import com.banula.openlib.ocpi.model.vo.Connector;
import com.banula.openlib.ocpi.model.vo.EVSE;
import com.banula.openlib.ocpi.util.Constants;
import com.banula.openlib.ocpi.util.ModelPatcherUtil;
import com.banula.openlib.mongodb.util.GenericMongoMapper;
import com.banula.navigationservice.config.MongoCollectionMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class NSPLocationServiceImpl implements NSPLocationService {

    private final SmartLocationRepository smartLocationRepository;
    private final LocationUtility locationUtility;
    private final MongoCollectionMapper mongoCollectionMapper;
    private final GenericMongoMapper genericMongoMapper;

    // returns either LocationDTO or EVSE or Connector object
    @Override
    public Object getLocationEvseConnector(String countryCode, String partyId, String locationId, String evseUid,
            String connectorId) {
        try {
            Optional<MongoSmartLocation> locationOpt = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, locationId);
            if (locationOpt.isEmpty()) {
                throw new OCPICustomException("Location not found");
            }

            MongoSmartLocation mongoSmartLocation = locationOpt.get();
            // MongoSmartLocation extends SmartLocation extends Location, so we can cast
            // directly
            Location location = mongoSmartLocation;

            // Case 1: No evseUid and no connectorId -> return LocationDTO
            if (evseUid == null && connectorId == null) {
                return LocationMapper.toLocationDTO(location);
            }

            // Case 2: evseUid is not null -> find EVSE
            EVSE evse = LocationUtility.findEvseInLocationByEvseUid(evseUid, location);
            if (evse == null) {
                throw new OCPICustomException("EVSE not found with uid: " + evseUid,
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            // If connectorId is null, return the EVSE
            if (connectorId == null) {
                return evse;
            }

            // Case 3: Both evseUid and connectorId are not null -> find Connector
            Connector connector = LocationUtility.findConnectorInEvseByConnectorId(connectorId, evse);
            if (connector == null) {
                throw new OCPICustomException("Connector not found with id: " + connectorId,
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            return connector;

        } catch (OCPICustomException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "Error happened while fetching location, error message: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putEvse(EVSE evseVO, String countryCode, String party_id, String locationId, String evseUid) {

        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationEvseConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // verify if the evseUid is already registered and replace object
        EVSE currentEVSE = null;
        if (locationDTO.getEvses() == null) {
            locationDTO.setEvses(new ArrayList<>());
        } else {
            currentEVSE = (EVSE) getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null);
        }
        if (currentEVSE != null) {
            locationDTO.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
        }

        // add the new evse to the ArrayList of evses in the Location and register
        // location
        locationDTO.getEvses().add(evseVO);
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    public void patchEvse(EVSE incompleteEvse, String countryCode, String party_id, String locationId, String evseUid) {
        try {

            Optional<MongoSmartLocation> optionalMongoSmartLocation = smartLocationRepository
                    .findByCompoundIndex(
                            countryCode, party_id, locationId);

            if (optionalMongoSmartLocation.isEmpty()) {
                throw new OCPICustomException("Location not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            if (getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null) == null) {
                throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            Location existingLocation = optionalMongoSmartLocation.get();
            EVSE existingEvseToUpdate = LocationUtility.findEvseInLocationByEvseUid(evseUid, existingLocation);
            ModelPatcherUtil.evsePatcher(existingEvseToUpdate, incompleteEvse);

            // substitute old EVSE with new EVSE and save location
            existingLocation.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
            existingLocation.getEvses().add(existingEvseToUpdate);

            MongoSmartLocation mongoSmartLocation = optionalMongoSmartLocation.get();
            // Copy properties from existingLocation to mongoSmartLocation and convert
            mongoSmartLocation = genericMongoMapper.toMongo(existingLocation, MongoSmartLocation.class);

            smartLocationRepository.save(mongoSmartLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putConnector(Connector connectorVO, String countryCode, String party_id, String locationId,
            String evseUid, String connectorId) {
        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationEvseConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // get the EVSE and current index
        EVSE evse = (EVSE) getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null);
        if (evse == null) {
            throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        int evseCurrentIndex = LocationUtility.evseIndexOf(locationDTO.getEvses(), evse);

        // verify if the connector is already registered and replace object, otherwise
        // add it
        if (evse.getConnectors() == null) {
            evse.setConnectors(new ArrayList<>());
        }
        int currentConnectorIndex = LocationUtility.connectorIndexOf(evse.getConnectors(), connectorVO);
        if (currentConnectorIndex != -1) {
            evse.getConnectors().set(currentConnectorIndex, connectorVO);
        } else {
            evse.getConnectors().add(connectorVO);
        }

        // replace the EVSE in the locationDTO
        locationDTO.getEvses().set(evseCurrentIndex, evse);

        // push the updated locationDTO to the database
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    @Override
    public void patchConnector(Connector incompleteConnector, String countryCode, String party_id, String locationId,
            String evseUid, String connectorId) {
        try {
            // verify if location EVSE and Connector exists
            Optional<MongoSmartLocation> optionalMongoSmartLocation = smartLocationRepository
                    .findByCompoundIndex(
                            countryCode, party_id, locationId);

            if (optionalMongoSmartLocation.isEmpty()) {
                throw new OCPICustomException("Location not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }
            if (getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null) == null) {
                throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }
            if (getLocationEvseConnector(countryCode, party_id, locationId, evseUid, connectorId) == null) {
                throw new OCPICustomException("Connector not found",
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            MongoSmartLocation mongoSmartLocation = optionalMongoSmartLocation.get();
            EVSE existingEvse = LocationUtility.findEvseInLocationByEvseUid(evseUid, mongoSmartLocation);
            Connector existingConnectorToUpdate = LocationUtility.findConnectorInEvseByConnectorId(connectorId,
                    existingEvse);

            ModelPatcherUtil.connectorPatcher(existingConnectorToUpdate, incompleteConnector);

            // substitute old Connector with new Connector and save location
            existingEvse.getConnectors().removeIf(connector -> connector.getId().equals(connectorId));
            existingEvse.getConnectors().add(existingConnectorToUpdate);

            mongoSmartLocation.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
            mongoSmartLocation.getEvses().add(existingEvse);
            // Convert to MongoEntity with updated timestamp
            mongoSmartLocation = genericMongoMapper.toMongo(mongoSmartLocation, MongoSmartLocation.class);

            smartLocationRepository.save(mongoSmartLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putLocation(LocationDTO locationDTO, String countryCode, String partyId, String ocpiId) {
        try {
            MongoSmartLocation mongoSmartLocation = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, ocpiId)
                    .orElse(null);
            Location location = LocationMapper.toLocationEntity(locationDTO);
            // Convert Location to MongoSmartLocation with smart upsert
            mongoSmartLocation = genericMongoMapper.toMongo(location, MongoSmartLocation.class);
            smartLocationRepository.save(mongoSmartLocation);
            log.info("Location saved in database! | uid: {} | collection: {}", locationDTO.getId(),
                    mongoCollectionMapper.getSmartLocationCollectionName());
        } catch (Exception e) {
            String errorMessage = "Error happened while pushing location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void patchLocation(LocationDTO locationDTO, String countryCode, String partyId, String id) {
        try {
            Location incompleteLocation = LocationMapper.toLocationEntity(locationDTO);
            MongoSmartLocation mongoExistingLocation = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, id)
                    .orElseThrow(RuntimeException::new);
            ModelPatcherUtil.locationPatcher(mongoExistingLocation, incompleteLocation);
            // Convert to MongoEntity with updated timestamp
            mongoExistingLocation = genericMongoMapper.toMongo(mongoExistingLocation, MongoSmartLocation.class);
            smartLocationRepository.save(mongoExistingLocation);
        } catch (Exception e) {
            String errorMessage = "Error happened while patching location: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public List<LocationDTO> findLocations(LocalDateTime dateFrom, LocalDateTime dateTo,
            Integer offset, Integer limit) {
        try {
            int safeOffset = offset == null ? 0 : offset;
            int safeLimit = limit == null ? 100 : limit;
            int page = safeOffset / safeLimit;
            Pageable pageable = PageRequest.of(page, safeLimit);
            return smartLocationRepository.findPublishedSmartLocations(dateFrom, dateTo, pageable)
                    .getContent()
                    .stream()
                    .map(mongoLoc -> (Location) mongoLoc) // MongoSmartLocation extends Location
                    .map(LocationMapper::toLocationDTO)
                    .toList();
        } catch (Exception e) {
            String errorMessage = "Error happened while fetching locations, error message: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

}
