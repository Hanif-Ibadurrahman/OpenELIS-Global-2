package org.openelisglobal.surveillance.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseEvaluation;

public interface DiseaseCaseEvaluationDAO {

    DiseaseCaseEvaluation save(DiseaseCaseEvaluation evaluation);

    Optional<DiseaseCaseEvaluation> findById(Integer id);

    List<DiseaseCaseEvaluation> findBySampleId(String sampleId);

    List<DiseaseCaseEvaluation> findByIcdCodeAndDateRange(String icdCode, Timestamp startDate, Timestamp endDate);

    List<Object[]> aggregateByIcdCode(Timestamp startDate, Timestamp endDate);

    List<Object[]> aggregateByIcdCodeAndRegion(Timestamp startDate, Timestamp endDate);

    long countByDefinitionIdSince(Integer definitionId, Timestamp since);
}
