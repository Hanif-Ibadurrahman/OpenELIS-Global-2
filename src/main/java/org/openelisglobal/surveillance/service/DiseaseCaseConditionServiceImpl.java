package org.openelisglobal.surveillance.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.dao.DiseaseCaseConditionDAO;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiseaseCaseConditionServiceImpl implements DiseaseCaseConditionService {

    @Autowired
    private DiseaseCaseConditionDAO conditionDAO;

    @Override
    public DiseaseCaseCondition save(DiseaseCaseCondition condition) {
        return conditionDAO.save(condition);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseCaseCondition> findById(Integer id) {
        return conditionDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseCaseCondition> findByDefinitionId(Integer definitionId) {
        return conditionDAO.findByDefinitionId(definitionId);
    }

    @Override
    public void delete(Integer id) {
        conditionDAO.findById(id).ifPresent(conditionDAO::delete);
    }

    @Override
    public void deleteByDefinitionId(Integer definitionId) {
        conditionDAO.deleteByDefinitionId(definitionId);
    }
}
