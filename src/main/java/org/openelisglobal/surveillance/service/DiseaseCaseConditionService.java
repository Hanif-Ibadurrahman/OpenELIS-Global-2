package org.openelisglobal.surveillance.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;

public interface DiseaseCaseConditionService {

    DiseaseCaseCondition save(DiseaseCaseCondition condition);

    Optional<DiseaseCaseCondition> findById(Integer id);

    List<DiseaseCaseCondition> findByDefinitionId(Integer definitionId);

    void delete(Integer id);

    void deleteByDefinitionId(Integer definitionId);
}
