package org.openelisglobal.surveillance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DiseaseCaseDefinitionDAOImpl implements DiseaseCaseDefinitionDAO {

    private static final Logger logger = LoggerFactory.getLogger(DiseaseCaseDefinitionDAOImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DiseaseCaseDefinition save(DiseaseCaseDefinition definition) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (definition.getCreatedAt() == null) {
            definition.setCreatedAt(now);
        }
        definition.setLastUpdated(now);
        if (definition.getId() == null) {
            entityManager.persist(definition);
            return definition;
        } else {
            return entityManager.merge(definition);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseDefinition> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(DiseaseCaseDefinition.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findAll() {
        return entityManager
                .createQuery("FROM DiseaseCaseDefinition d ORDER BY d.icdCode, d.version DESC",
                        DiseaseCaseDefinition.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findAllActive() {
        return entityManager
                .createQuery(
                        "FROM DiseaseCaseDefinition d WHERE d.isActive = true ORDER BY d.icdCode, d.version DESC",
                        DiseaseCaseDefinition.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findByIcdCode(String icdCode) {
        return entityManager
                .createQuery("FROM DiseaseCaseDefinition d WHERE d.icdCode = :icdCode ORDER BY d.version DESC",
                        DiseaseCaseDefinition.class)
                .setParameter("icdCode", icdCode).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseDefinition> findActiveByIcdCode(String icdCode) {
        try {
            List<DiseaseCaseDefinition> results = entityManager
                    .createQuery(
                            "FROM DiseaseCaseDefinition d WHERE d.icdCode = :icdCode AND d.isActive = true ORDER BY d.version DESC",
                            DiseaseCaseDefinition.class)
                    .setParameter("icdCode", icdCode).setMaxResults(1).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding active case definition for ICD code: {}", icdCode, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(DiseaseCaseDefinition definition) {
        entityManager.remove(entityManager.contains(definition) ? definition : entityManager.merge(definition));
    }
}
