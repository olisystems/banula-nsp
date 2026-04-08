package com.banula.navigationservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GenericMongoMapper {

    @Autowired
    private MongoTemplate mongoTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Core conversions with smart upsert
    public <T, M> M toMongo(T entity, Class<M> mongoClass) {
        try {
            M mongoEntity = mongoClass.getDeclaredConstructor().newInstance();
            CustomBeanUtils.copyProperties(entity, mongoEntity);

            // Get compound index annotation
            CompoundIndex compoundIndex = getCompoundIndexAnnotation(mongoClass);
            validateUniqueIndex(compoundIndex);

            // Parse business key fields from index definition
            List<String> businessKeyFields = parseIndexFieldNames(compoundIndex.def());

            // Find existing document using business key fields
            M existingDocument = findExistingDocument(mongoEntity, mongoClass, businessKeyFields);
            
            // If existing document found, copy its mongoId
            if (existingDocument != null) {
                Object existingId = getFieldIfExists(existingDocument, "mongoId");
                if (existingId != null) {
                    setFieldIfExists(mongoEntity, "mongoId", existingId);
                }
            }

            // Optionally set lastUpdated if field exists
            setFieldIfExists(mongoEntity, "lastUpdated", LocalDateTime.now());

            return mongoEntity;
        } catch (Exception e) {
            throw new RuntimeException("Error converting entity to mongo entity: " + e.getMessage(), e);
        }
    }

    public <T, M> T toEntity(M mongoEntity, Class<T> entityClass) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            CustomBeanUtils.copyProperties(mongoEntity, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error converting mongo entity to entity: " + e.getMessage(), e);
        }
    }

    public <T, D> D toDTO(T entity, Class<D> dtoClass) {
        try {
            D dto = dtoClass.getDeclaredConstructor().newInstance();
            CustomBeanUtils.copyProperties(entity, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Error converting entity to DTO: " + e.getMessage(), e);
        }
    }

    public <T, D> T fromDTO(D dto, Class<T> entityClass) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            CustomBeanUtils.copyProperties(dto, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error converting DTO to entity: " + e.getMessage(), e);
        }
    }

    // Composite conversions
    public <M, D, T> D mongoToDTO(M mongoEntity, Class<T> entityClass, Class<D> dtoClass) {
        T entity = toEntity(mongoEntity, entityClass);
        return toDTO(entity, dtoClass);
    }

    public <D, M, T> M dtoToMongo(D dto, Class<T> entityClass, Class<M> mongoClass) {
        T entity = fromDTO(dto, entityClass);
        return toMongo(entity, mongoClass);
    }

    // List variants
    public <T, M> List<M> toMongoList(List<T> entities, Class<M> mongoClass) {
        List<M> mongoEntities = new ArrayList<>();
        for (T entity : entities) {
            mongoEntities.add(toMongo(entity, mongoClass));
        }
        return mongoEntities;
    }

    public <T, M> List<T> toEntityList(List<M> mongoEntities, Class<T> entityClass) {
        List<T> entities = new ArrayList<>();
        for (M mongoEntity : mongoEntities) {
            entities.add(toEntity(mongoEntity, entityClass));
        }
        return entities;
    }

    public <T, D> List<D> toDTOList(List<T> entities, Class<D> dtoClass) {
        List<D> dtos = new ArrayList<>();
        for (T entity : entities) {
            dtos.add(toDTO(entity, dtoClass));
        }
        return dtos;
    }

    public <T, D> List<T> fromDTOList(List<D> dtos, Class<T> entityClass) {
        List<T> entities = new ArrayList<>();
        for (D dto : dtos) {
            entities.add(fromDTO(dto, entityClass));
        }
        return entities;
    }

    public <M, D, T> List<D> mongoListToDTO(List<M> mongoEntities, Class<T> entityClass, Class<D> dtoClass) {
        List<D> dtos = new ArrayList<>();
        for (M mongoEntity : mongoEntities) {
            dtos.add(mongoToDTO(mongoEntity, entityClass, dtoClass));
        }
        return dtos;
    }

    public <D, M, T> List<M> dtoListToMongo(List<D> dtos, Class<T> entityClass, Class<M> mongoClass) {
        List<M> mongoEntities = new ArrayList<>();
        for (D dto : dtos) {
            mongoEntities.add(dtoToMongo(dto, entityClass, mongoClass));
        }
        return mongoEntities;
    }

    // Internal helper methods

    private CompoundIndex getCompoundIndexAnnotation(Class<?> mongoClass) {
        CompoundIndex annotation = mongoClass.getAnnotation(CompoundIndex.class);
        if (annotation == null) {
            throw new IllegalStateException(
                "MongoEntity class " + mongoClass.getSimpleName() + 
                " must have @CompoundIndex annotation with unique=true"
            );
        }
        return annotation;
    }

    private void validateUniqueIndex(CompoundIndex compoundIndex) {
        if (!compoundIndex.unique()) {
            throw new IllegalStateException(
                "@CompoundIndex must have unique=true for smart upsert logic to work"
            );
        }
    }

    private List<String> parseIndexFieldNames(String indexDefinition) {
        try {
            // Convert MongoDB index def format to valid JSON
            // Example: "{'countryCode': 1, 'partyId': 1, 'id': 1}" -> {"countryCode": 1, "partyId": 1, "id": 1}
            String jsonString = indexDefinition.replace("'", "\"");
            
            // Parse JSON to extract field names
            @SuppressWarnings("unchecked")
            Map<String, Object> indexMap = objectMapper.readValue(jsonString, Map.class);
            
            return new ArrayList<>(indexMap.keySet());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing index definition: " + indexDefinition, e);
        }
    }

    private <M> M findExistingDocument(M mongoEntity, Class<M> mongoClass, List<String> businessKeyFields) {
        Query query = buildBusinessKeyQuery(mongoEntity, businessKeyFields);
        return mongoTemplate.findOne(query, mongoClass);
    }

    private Query buildBusinessKeyQuery(Object mongoEntity, List<String> businessKeyFields) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();

        for (String fieldName : businessKeyFields) {
            Object fieldValue = getFieldIfExists(mongoEntity, fieldName);
            if (fieldValue != null) {
                criteriaList.add(Criteria.where(fieldName).is(fieldValue));
            }
        }

        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        return new Query(criteria);
    }

    private void setFieldIfExists(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Exception e) {
            // Gracefully skip if field doesn't exist or can't be set
        }
    }

    private Object getFieldIfExists(Object source, String fieldName) {
        try {
            Field field = findField(source.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(source);
            }
        } catch (Exception e) {
            // Return null if field doesn't exist or can't be accessed
        }
        return null;
    }

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
