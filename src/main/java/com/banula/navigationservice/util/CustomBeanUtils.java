package com.banula.navigationservice.util;

import org.springframework.beans.BeanUtils;

import com.banula.navigationservice.model.HasCompositeMongoId;

public class CustomBeanUtils {

    public static void copyProperties(Object origin, Object destination) {
        BeanUtils.copyProperties(origin, destination);

        if (destination instanceof HasCompositeMongoId) {
            if (((HasCompositeMongoId) destination).getCountryCode() != null &&
                    ((HasCompositeMongoId) destination).getPartyId() != null &&
                    ((HasCompositeMongoId) destination).getId() != null) {
                String mongoid = String.format("%s*%s*%s",
                        ((HasCompositeMongoId) destination).getCountryCode(),
                        ((HasCompositeMongoId) destination).getPartyId(),
                        ((HasCompositeMongoId) destination).getId());
                ((HasCompositeMongoId) destination).setMongoId(mongoid);
            }
        }

    }
}
