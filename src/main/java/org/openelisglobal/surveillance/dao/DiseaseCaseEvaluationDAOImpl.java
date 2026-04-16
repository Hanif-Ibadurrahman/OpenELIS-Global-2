package org.openelisglobal.surveillance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseEvaluation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DiseaseCaseEvaluationDAOImpl implements DiseaseCaseEvaluationDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DiseaseCaseEvaluation save(DiseaseCaseEvaluation evaluation) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (evaluation.getEvaluationDate() == null) {
            evaluation.setEvaluationDate(now);
        }
        evaluation.setLastUpdated(now);
        if (evaluation.getId() == null) {
            entityManager.persist(evaluation);
            return evaluation;
        } else {
            return entityManager.merge(evaluation);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseEvaluation> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(DiseaseCaseEvaluation.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseEvaluation> findBySampleId(String sampleId) {
        return entityManager
                .createQuery("FROM DiseaseCaseEvaluation e WHERE e.sampleId = :sampleId ORDER BY e.evaluationDate DESC",
                        DiseaseCaseEvaluation.class)
                .setParameter("sampleId", sampleId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseEvaluation> findByIcdCodeAndDateRange(String icdCode, Timestamp startDate,
            Timestamp endDate) {
        String hql = "FROM DiseaseCaseEvaluation e WHERE e.icdCode = :icdCode AND e.isConfirmed = true";
        if (startDate != null) hql += " AND e.evaluationDate >= :startDate";
        if (endDate != null) hql += " AND e.evaluationDate <= :endDate";
        hql += " ORDER BY e.evaluationDate DESC";

        var query = entityManager.createQuery(hql, DiseaseCaseEvaluation.class).setParameter("icdCode", icdCode);
        if (startDate != null) query.setParameter("startDate", startDate);
        if (endDate != null) query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateByIcdCode(Timestamp startDate, Timestamp endDate) {
        String hql = "SELECT e.icdCode, COUNT(e.id) FROM DiseaseCaseEvaluation e WHERE e.isConfirmed = true";
        if (startDate != null) hql += " AND e.evaluationDate >= :startDate";
        if (endDate != null) hql += " AND e.evaluationDate <= :endDate";
        hql += " GROUP BY e.icdCode ORDER BY COUNT(e.id) DESC";

        var query = entityManager.createQuery(hql, Object[].class);
        if (startDate != null) query.setParameter("startDate", startDate);
        if (endDate != null) query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> aggregateByIcdCodeAndRegion(Timestamp startDate, Timestamp endDate) {
        String hql = "SELECT e.icdCode, e.regionId, COUNT(e.id) FROM DiseaseCaseEvaluation e WHERE e.isConfirmed = true";
        if (startDate != null) hql += " AND e.evaluationDate >= :startDate";
        if (endDate != null) hql += " AND e.evaluationDate <= :endDate";
        hql += " GROUP BY e.icdCode, e.regionId ORDER BY e.icdCode, COUNT(e.id) DESC";

        var query = entityManager.createQuery(hql, Object[].class);
        if (startDate != null) query.setParameter("startDate", startDate);
        if (endDate != null) query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDefinitionIdSince(Integer definitionId, Timestamp since) {
        String hql = "SELECT COUNT(e.id) FROM DiseaseCaseEvaluation e "
                + "WHERE e.definitionId = :defId AND e.isConfirmed = true";
        if (since != null) hql += " AND e.evaluationDate >= :since";
        var query = entityManager.createQuery(hql, Long.class).setParameter("defId", definitionId);
        if (since != null) query.setParameter("since", since);
        return query.getSingleResult();
    }
}
