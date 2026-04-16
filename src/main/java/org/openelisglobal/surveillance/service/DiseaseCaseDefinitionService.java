package org.openelisglobal.surveillance.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;

public interface DiseaseCaseDefinitionService {

    DiseaseCaseDefinition save(DiseaseCaseDefinition definition);

    Optional<DiseaseCaseDefinition> findById(Integer id);

    List<DiseaseCaseDefinition> findAll();

    List<DiseaseCaseDefinition> findAllActive();

    List<DiseaseCaseDefinition> findByIcdCode(String icdCode);

    Optional<DiseaseCaseDefinition> findActiveByIcdCode(String icdCode);

    void delete(Integer id);
}
