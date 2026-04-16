package org.openelisglobal.surveillance.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;

public interface DiseaseNotificationRecipientService {

    DiseaseNotificationRecipient save(DiseaseNotificationRecipient recipient);

    Optional<DiseaseNotificationRecipient> findById(Integer id);

    List<DiseaseNotificationRecipient> findByDefinitionId(Integer definitionId);

    void delete(Integer id);

    void deleteByDefinitionId(Integer definitionId);

    List<DiseaseNotificationRecipient> replaceForDefinition(Integer definitionId,
            List<DiseaseNotificationRecipient> recipients);
}
