package com.banula.navigationservice.repository;

import java.util.Optional;

/**
 * Custom repository fragment for querying by compound index fields.
 * This interface can be extended by any MongoRepository to add generic compound index query support.
 */
public interface CompoundIndexRepository<T> {
    
    /**
     * Find a document by its compound index fields (defined in @CompoundIndex annotation).
     * Automatically extracts business key field values from the entity and queries MongoDB.
     * 
     * @param entity The entity instance containing the business key field values
     * @return Optional containing the found document or empty if not found
     */
    Optional<T> findByCompoundIndex(T entity);
}
