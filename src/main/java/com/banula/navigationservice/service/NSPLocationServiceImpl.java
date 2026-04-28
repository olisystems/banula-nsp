package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.custom.smartlocations.SmartLocationState;
import com.banula.openlib.ocpi.custom.smartlocations.mongo.MongoSmartLocation;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.openlib.ocpi.util.LocationUtility;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import com.banula.openlib.ocpi.model.Location;
import com.banula.openlib.ocpi.model.dto.ConnectorDTO;
import com.banula.openlib.ocpi.model.dto.EvseDTO;
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
                return genericMongoMapper.locationToDTO(location);
            }

            // Case 2: evseUid is not null -> find EVSE
            EVSE evse = LocationUtility.findEvseInLocationByEvseUid(evseUid, location);
            if (evse == null) {
                throw new OCPICustomException("EVSE not found with uid: " + evseUid,
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            // If connectorId is null, return the EVSE as DTO
            if (connectorId == null) {
                return genericMongoMapper.evseToDTO(evse);
            }

            // Case 3: Both evseUid and connectorId are not null -> find Connector
            Connector connector = LocationUtility.findConnectorInEvseByConnectorId(connectorId, evse);
            if (connector == null) {
                throw new OCPICustomException("Connector not found with id: " + connectorId,
                        Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
            }

            return genericMongoMapper.connectorToDTO(connector);

        } catch (OCPICustomException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "Error happened while fetching location, error message: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

    @Override
    public void putEvse(EvseDTO evseVO, String countryCode, String party_id, String locationId, String evseUid) {

        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationEvseConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // verify if the evseUid is already registered and replace object
        EvseDTO currentEVSE = null;
        if (locationDTO.getEvses() == null) {
            locationDTO.setEvses(new ArrayList<>());
        } else {
            currentEVSE = (EvseDTO) getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null);
        }
        if (currentEVSE != null) {
            locationDTO.getEvses().removeIf(evse -> evse.getUid().equals(evseUid));
        }

        // add the new evse to the ArrayList of evses in the Location and register
        // location
        locationDTO.getEvses().add(evseVO);
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    @Override
    public void patchEvse(EvseDTO incompleteEvseDto, String countryCode, String party_id, String locationId,
            String evseUid) {
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
            EVSE incompleteEvse = genericMongoMapper.evseFromDTO(incompleteEvseDto);
            ModelPatcherUtil.evsePatcher(existingLocation, existingEvseToUpdate, incompleteEvse);

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
    public void putConnector(ConnectorDTO connectorVO, String countryCode, String party_id, String locationId,
            String evseUid, String connectorId) {
        // get the locationDTO
        LocationDTO locationDTO = (LocationDTO) getLocationEvseConnector(countryCode, party_id, locationId, null, null);
        if (locationDTO == null) {
            throw new OCPICustomException("Location not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }

        // get the EVSE DTO and current index
        EvseDTO evseDTO = (EvseDTO) getLocationEvseConnector(countryCode, party_id, locationId, evseUid, null);
        if (evseDTO == null) {
            throw new OCPICustomException("EVSE not found", Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS);
        }
        int evseCurrentIndex = LocationUtility.evseDTOIndexOf(locationDTO.getEvses(), evseDTO);

        // verify if the connector is already registered and replace object, otherwise
        // add it
        if (evseDTO.getConnectors() == null) {
            evseDTO.setConnectors(new ArrayList<>());
        }
        int currentConnectorIndex = LocationUtility.connectorDTOIndexOf(evseDTO.getConnectors(), connectorVO);
        if (currentConnectorIndex != -1) {
            evseDTO.getConnectors().set(currentConnectorIndex, connectorVO);
        } else {
            evseDTO.getConnectors().add(connectorVO);
        }

        // replace the EVSE in the locationDTO
        locationDTO.getEvses().set(evseCurrentIndex, evseDTO);

        // push the updated locationDTO to the database
        putLocation(locationDTO, countryCode, party_id, locationId);
    }

    @Override
    public void patchConnector(ConnectorDTO incompleteConnectorDTO, String countryCode, String party_id,
            String locationId,
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
            Connector incompleteConnector = genericMongoMapper.connectorFromDTO(incompleteConnectorDTO);

            ModelPatcherUtil.connectorPatcher(mongoSmartLocation, existingEvse, existingConnectorToUpdate,
                    incompleteConnector);

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
            Optional<MongoSmartLocation> mongoSmartLocationOpt = smartLocationRepository
                    .findByCompoundIndex(countryCode, partyId, ocpiId);

            if (mongoSmartLocationOpt.isPresent()) {
                log.info("Location already exists, patching instead of putting | id: {}", ocpiId);
                patchLocation(locationDTO, countryCode, partyId, ocpiId);
                return;
            }

            Location location = genericMongoMapper.locationFromDTO(locationDTO);
            // Convert Location to MongoSmartLocation with smart upsert
            MongoSmartLocation mongoSmartLocation = genericMongoMapper.toMongo(location, MongoSmartLocation.class);
            mongoSmartLocation.setSmartLocationState(SmartLocationState.PLAIN_OCPI);
            mongoSmartLocation.setPublish(false);
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
            Location incompleteLocation = genericMongoMapper.locationFromDTO(locationDTO);
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
            int safeLimit = (limit == null || limit <= 0) ? 100 : limit;
            int remainder = safeOffset % safeLimit;
            int page = safeOffset / safeLimit;
            // Fetch safeLimit + remainder items so that after skipping `remainder` we have
            // exactly safeLimit items for non-page-aligned offsets
            int pageSize = safeLimit + remainder;
            Pageable pageable = PageRequest.of(page, pageSize);
            List<LocationDTO> items = smartLocationRepository.findVerifiedSmartLocations(dateFrom, dateTo, pageable)
                    .getContent()
                    .stream()
                    .map(mongoLoc -> (Location) mongoLoc) // MongoSmartLocation extends Location
                    .map(genericMongoMapper::locationToDTO)
                    .toList();
            if (remainder == 0) {
                return items;
            }
            int fromIndex = Math.min(remainder, items.size());
            int toIndex = Math.min(fromIndex + safeLimit, items.size());
            return items.subList(fromIndex, toIndex);
        } catch (Exception e) {
            String errorMessage = "Error happened while fetching locations, error message: " + e.getLocalizedMessage();
            log.info(errorMessage);
            throw new OCPICustomException(errorMessage);
        }
    }

}
