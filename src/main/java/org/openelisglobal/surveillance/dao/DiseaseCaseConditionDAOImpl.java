package org.openelisglobal.surveillance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DiseaseCaseConditionDAOImpl implements DiseaseCaseConditionDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DiseaseCaseCondition save(DiseaseCaseCondition condition) {
        if (condition.getLastUpdated() == null) {
            condition.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        }
        if (condition.getId() == null) {
            entityManager.persist(condition);
            return condition;
        } else {
            return entityManager.merge(condition);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseCondition> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(DiseaseCaseCondition.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseCondition> findByDefinitionId(Integer definitionId) {
        return entityManager
                .createQuery(
                        "FROM DiseaseCaseCondition c WHERE c.definition.id = :defId ORDER BY c.ruleGroup ASC, c.id ASC",
                        DiseaseCaseCondition.class)
                .setParameter("defId", definitionId).getResultList();
    }

    @Override
    public void delete(DiseaseCaseCondition condition) {
        entityManager.remove(entityManager.contains(condition) ? condition : entityManager.merge(condition));
    }

    @Override
    public void deleteByDefinitionId(Integer definitionId) {
        entityManager.createQuery("DELETE FROM DiseaseCaseCondition c WHERE c.definition.id = :defId")
                .setParameter("defId", definitionId).executeUpdate();
    }
}
