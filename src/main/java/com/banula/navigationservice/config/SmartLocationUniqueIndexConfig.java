package com.banula.navigationservice.config;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A MeLo or MaLo belongs to one smart location: two locations sharing one would read the same
 * meter readings and settle the same energy twice. Each of {@link #FIELDS} gets a unique index,
 * partial (only where the value is set: a PLAIN_OCPI location has none). Created at every start
 * once the data allows it: while a duplicate is still stored the index is skipped and the
 * conflicting locations are logged as ERROR, so the service starts all the same and the index is
 * built on the first start after the data is fixed. Until then the smart location update rejects
 * a value another location already holds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartLocationUniqueIndexConfig implements InitializingBean {

    static final List<String> FIELDS = List.of("meteringLocationId", "marketLocationId", "malo");

    private final MongoTemplate mongo;
    private final MongoCollectionMapper collections;

    @Override
    public void afterPropertiesSet() {
        String collection = collections.getSmartLocationCollectionName();
        for (String field : FIELDS) {
            try {
                List<Document> duplicates = duplicates(collection, field);
                if (!duplicates.isEmpty()) {
                    log.error("Unique index on {}.{} not created: {} value(s) are used by several smart locations: {}",
                            collection, field, duplicates.size(), duplicates);
                    continue;
                }
                mongo.indexOps(collection).ensureIndex(new Index()
                        .on(field, Sort.Direction.ASC)
                        .unique()
                        .named("unique_" + field)
                        .partial(PartialIndexFilter.of(setValue(field))));
            } catch (Exception e) {
                log.error("Unique index on {}.{} not created: {}", collection, field, e.getMessage(), e);
            }
        }
    }

    /** Only documents whose field holds a non-empty string take part in the index. */
    static Document setValue(String field) {
        return new Document(field, new Document("$type", "string").append("$gt", ""));
    }

    /** Each value used by more than one smart location, with those locations. */
    private List<Document> duplicates(String collection, String field) {
        List<Document> pipeline = List.of(
                new Document("$match", setValue(field)),
                new Document("$group", new Document("_id", "$" + field)
                        .append("locations", new Document("$push", new Document("$concat", List.of(
                                new Document("$ifNull", List.of("$countryCode", "?")), "/",
                                new Document("$ifNull", List.of("$partyId", "?")), "/",
                                new Document("$ifNull", List.of("$id", "?"))))))
                        .append("count", new Document("$sum", 1))),
                new Document("$match", new Document("count", new Document("$gt", 1))));
        return mongo.getCollection(collection).aggregate(pipeline).into(new ArrayList<>());
    }
}
