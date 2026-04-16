package org.openelisglobal.surveillance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.dao.DiseaseCaseDefinitionDAO;
import org.openelisglobal.surveillance.dao.DiseaseNotificationRecipientDAO;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiseaseNotificationRecipientServiceImpl implements DiseaseNotificationRecipientService {

    @Autowired
    private DiseaseNotificationRecipientDAO recipientDAO;

    @Autowired
    private DiseaseCaseDefinitionDAO definitionDAO;

    @Override
    public DiseaseNotificationRecipient save(DiseaseNotificationRecipient recipient) {
        return recipientDAO.save(recipient);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseNotificationRecipient> findById(Integer id) {
        return recipientDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseNotificationRecipient> findByDefinitionId(Integer definitionId) {
        return recipientDAO.findByDefinitionId(definitionId);
    }

    @Override
    public void delete(Integer id) {
        recipientDAO.findById(id).ifPresent(recipientDAO::delete);
    }

    @Override
    public void deleteByDefinitionId(Integer definitionId) {
        recipientDAO.deleteByDefinitionId(definitionId);
    }

    @Override
    public List<DiseaseNotificationRecipient> replaceForDefinition(Integer definitionId,
            List<DiseaseNotificationRecipient> recipients) {
        recipientDAO.deleteByDefinitionId(definitionId);
        Optional<DiseaseCaseDefinition> definitionOpt = definitionDAO.findById(definitionId);
        if (definitionOpt.isEmpty()) {
            return new ArrayList<>();
        }
        DiseaseCaseDefinition definition = definitionOpt.get();
        List<DiseaseNotificationRecipient> saved = new ArrayList<>();
        if (recipients != null) {
            for (DiseaseNotificationRecipient r : recipients) {
                r.setId(null);
                r.setDefinition(definition);
                saved.add(recipientDAO.save(r));
            }
        }
        return saved;
    }
}
