package com.banula.navigationservice.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of CompoundIndexRepository that provides generic compound
 * index query support.
 * This class is automatically picked up by Spring Data MongoDB for repositories
 * that extend CompoundIndexRepository.
 */
public class CompoundIndexRepositoryImpl<T> implements CompoundIndexRepository<T> {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Optional<T> findByCompoundIndex(T entity) {
        if (entity == null) {
            return Optional.empty();
        }

        try {
            Class<?> entityClass = entity.getClass();

            // Get the @CompoundIndex annotation
            CompoundIndex compoundIndex = entityClass.getAnnotation(CompoundIndex.class);
            if (compoundIndex == null) {
                throw new IllegalStateException(
                        "Entity class " + entityClass.getSimpleName() +
                                " must have @CompoundIndex annotation");
            }

            // Parse the compound index definition to extract field names
            List<String> businessKeyFields = parseIndexFieldNames(compoundIndex.def());

            // Build the query using reflection to get field values from the entity
            Query query = buildBusinessKeyQuery(entity, businessKeyFields);

            // Execute the query
            @SuppressWarnings("unchecked")
            T result = (T) mongoTemplate.findOne(query, entityClass);

            return Optional.ofNullable(result);

        } catch (Exception e) {
            throw new RuntimeException("Error finding document by compound index: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the compound index definition string to extract field names.
     * Example: "{'countryCode': 1, 'partyId': 1, 'id': 1}" -> ["countryCode",
     * "partyId", "id"]
     */
    private List<String> parseIndexFieldNames(String indexDef) {
        List<String> fieldNames = new ArrayList<>();
        try {
            // Convert MongoDB format (single quotes) to valid JSON (double quotes)
            String validJson = indexDef.replace("'", "\"");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(validJson);

            Iterator<String> fieldNamesIterator = rootNode.fieldNames();
            while (fieldNamesIterator.hasNext()) {
                fieldNames.add(fieldNamesIterator.next());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse compound index definition: " + indexDef, e);
        }
        return fieldNames;
    }

    /**
     * Build a MongoDB query using the business key fields extracted from the
     * entity.
     */
    private Query buildBusinessKeyQuery(Object entity, List<String> businessKeyFields) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();

        for (String fieldName : businessKeyFields) {
            Object fieldValue = getFieldValue(entity, fieldName);
            if (fieldValue != null) {
                criteriaList.add(Criteria.where(fieldName).is(fieldValue));
            }
        }

        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        return new Query(criteria);
    }

    /**
     * Get field value from an object using reflection, searching through
     * inheritance hierarchy.
     */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            // Field not found or not accessible, return null
        }
        return null;
    }

    /**
     * Find a field in a class or its superclasses.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}
