package com.banula.navigationservice.util;

import com.banula.navigationservice.model.MongoSmartLocation;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoLocationEventListener extends AbstractMongoEventListener<MongoSmartLocation> {

    private final CompositeIdAutoGenerator generator = new CompositeIdAutoGenerator();

    @Override
    public void onBeforeConvert(BeforeConvertEvent<MongoSmartLocation> event) {
        generator.assignMongoId(event.getSource());
    }

}
