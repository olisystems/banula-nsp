package com.banula.navigationservice.util;


import com.banula.navigationservice.model.MongoSmartLocation;

public class CompositeIdAutoGenerator {

    public void assignMongoId(MongoSmartLocation entity) {
        if (entity.getMongoId() == null &&
                entity.getCountryCode() != null &&
                entity.getPartyId() != null &&
                entity.getId() != null) {

            String id = String.format("%s*%s*%s",
                    entity.getCountryCode(),
                    entity.getPartyId(),
                    entity.getId());

            entity.setMongoId(id);
        }
    }

}
