package org.openelisglobal.surveillance.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.dao.DiseaseCaseDefinitionDAO;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiseaseCaseDefinitionServiceImpl implements DiseaseCaseDefinitionService {

    @Autowired
    private DiseaseCaseDefinitionDAO definitionDAO;

    @Override
    public DiseaseCaseDefinition save(DiseaseCaseDefinition definition) {
        return definitionDAO.save(definition);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseDefinition> findById(Integer id) {
        return definitionDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findAll() {
        return definitionDAO.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findAllActive() {
        return definitionDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseDefinition> findByIcdCode(String icdCode) {
        return definitionDAO.findByIcdCode(icdCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseDefinition> findActiveByIcdCode(String icdCode) {
        return definitionDAO.findActiveByIcdCode(icdCode);
    }

    @Override
    public void delete(Integer id) {
        definitionDAO.findById(id).ifPresent(definitionDAO::delete);
    }
}
