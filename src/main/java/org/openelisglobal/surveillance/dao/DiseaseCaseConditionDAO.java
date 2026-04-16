package org.openelisglobal.surveillance.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;

public interface DiseaseCaseConditionDAO {

    DiseaseCaseCondition save(DiseaseCaseCondition condition);

    Optional<DiseaseCaseCondition> findById(Integer id);

    List<DiseaseCaseCondition> findByDefinitionId(Integer definitionId);

    void delete(DiseaseCaseCondition condition);

    void deleteByDefinitionId(Integer definitionId);
}
