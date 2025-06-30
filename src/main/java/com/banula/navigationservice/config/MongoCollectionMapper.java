package com.banula.navigationservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("MongoCollectionMapper")
@Getter
public class MongoCollectionMapper {

    private final String ocnCredentialsCollectionName;
    private final String smartLocationCollectionName;

    @Autowired
    public MongoCollectionMapper(ApplicationConfiguration config) {
        String prefix = config.getCollectionPrefix();
        this.smartLocationCollectionName = prefix + "Location";
        this.ocnCredentialsCollectionName = prefix + "OcnCredentials";
    }
}
