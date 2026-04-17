package com.banula.navigationservice.util;

import org.springframework.beans.BeanUtils;

import com.banula.navigationservice.model.HasCompositeMongoId;

public class CustomBeanUtils {

    public static void copyProperties(Object origin, Object destination) {
        BeanUtils.copyProperties(origin, destination);

        if (destination instanceof HasCompositeMongoId) {
            HasCompositeMongoId target = (HasCompositeMongoId) destination;
            if (target.getCountryCode() != null &&
                    target.getPartyId() != null &&
                    target.getId() != null) {
                String existingMongoId = target.getMongoId();
                if (existingMongoId == null || existingMongoId.isBlank()) {
                    String mongoid = String.format("%s*%s*%s",
                            target.getCountryCode(),
                            target.getPartyId(),
                            target.getId());
                    target.setMongoId(mongoid);
                }
            }
        }

    }
}
