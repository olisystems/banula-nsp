package com.banula.navigationservice.mapper;

import com.banula.navigationservice.model.MongoSmartLocation;
import com.banula.navigationservice.util.CustomBeanUtils;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocation;

import java.util.List;

public class SmartLocationMapper {

    public static MongoSmartLocation toMongoSmartLocation(SmartLocation smartLocation) {
        if (smartLocation == null) {
            return null;
        }
        MongoSmartLocation mongoSmartLocation = new MongoSmartLocation();
        CustomBeanUtils.copyProperties(smartLocation, mongoSmartLocation);
        return mongoSmartLocation;
    }

    public static SmartLocation toSmartLocationEntity(MongoSmartLocation mongoSmartLocation) {
        if (mongoSmartLocation == null) {
            return null;
        }
        SmartLocation smartLocation = new SmartLocation();
        CustomBeanUtils.copyProperties(mongoSmartLocation, smartLocation);
        if (smartLocation.getId() == null) {
            smartLocation.setId(mongoSmartLocation.getMongoId());
        }
        return smartLocation;
    }

    public static MongoSmartLocation toMongoSmartLocation(SmartLocationDTO smartLocationDTO) {
        if (smartLocationDTO == null) {
            return null;
        }
        SmartLocation smartLocation = SmartLocationMapper.toSmartLocationEntity(smartLocationDTO);
        MongoSmartLocation mongoSmartLocation = new MongoSmartLocation();
        CustomBeanUtils.copyProperties(smartLocation, mongoSmartLocation);

        return mongoSmartLocation;
    }

    public static SmartLocationDTO toSmartLocationDTO(MongoSmartLocation mongoSmartLocation) {
        if (mongoSmartLocation == null) {
            return null;
        }
        SmartLocation smartLocation = toSmartLocationEntity(mongoSmartLocation);
        if (smartLocation.getId() == null) {
            smartLocation.setId(mongoSmartLocation.getMongoId());
        }
        return SmartLocationMapper.toSmartLocationDTO(smartLocation);
    }

    public static SmartLocation toSmartLocationEntity(SmartLocationDTO smartLocationDTO) {
        if (smartLocationDTO == null) {
            return null;
        }
        SmartLocation smartLocation = new SmartLocation();
        CustomBeanUtils.copyProperties(smartLocationDTO, smartLocation);
        return smartLocation;
    }

    public static SmartLocationDTO toSmartLocationDTO(SmartLocation smartLocation) {
        if (smartLocation == null) {
            return null;
        }
        SmartLocationDTO smartLocationDTO = new SmartLocationDTO();
        CustomBeanUtils.copyProperties(smartLocation, smartLocationDTO);
        return smartLocationDTO;
    }

    public static List<SmartLocationDTO> toListSmartLocationDTO(List<SmartLocation> smartLocationList) {
        if (smartLocationList == null) {
            return null;
        }
        return smartLocationList.stream()
                .map(SmartLocationMapper::toSmartLocationDTO)
                .toList();
    }

    public static List<SmartLocationDTO> toListSmartLocationDTOFromMongo(
            List<MongoSmartLocation> mongoSmartLocationList) {
        if (mongoSmartLocationList == null) {
            return null;
        }
        return mongoSmartLocationList.stream()
                .map(SmartLocationMapper::toSmartLocationDTO)
                .toList();
    }

    public static List<MongoSmartLocation> toListMongoSmartLocationFromDtoList(
            List<SmartLocationDTO> smartLocationDTOList) {
        if (smartLocationDTOList == null) {
            return null;
        }
        return smartLocationDTOList.stream()
                .map(SmartLocationMapper::toMongoSmartLocation)
                .toList();
    }

    public static List<MongoSmartLocation> toListMongoSmartLocationFromEntityList(
            List<SmartLocation> smartLocationList) {
        if (smartLocationList == null) {
            return null;
        }
        return smartLocationList.stream()
                .map(SmartLocationMapper::toMongoSmartLocation)
                .toList();
    }

    public static List<SmartLocationDTO> toListSmartLocationDTOFromMongoList(
            List<MongoSmartLocation> mongoSmartLocationList) {
        if (mongoSmartLocationList == null) {
            return null;
        }
        return mongoSmartLocationList.stream()
                .map(SmartLocationMapper::toSmartLocationDTO)
                .toList();
    }

    public static List<SmartLocation> toListSmartLocationFromMongoList(
            List<MongoSmartLocation> mongoSmartLocationList) {
        if (mongoSmartLocationList == null) {
            return null;
        }
        return mongoSmartLocationList.stream()
                .map(SmartLocationMapper::toSmartLocationEntity)
                .toList();
    }

}