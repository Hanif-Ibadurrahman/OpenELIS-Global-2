package org.openelisglobal.surveillance.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;

public interface DiseaseNotificationRecipientDAO {

    DiseaseNotificationRecipient save(DiseaseNotificationRecipient recipient);

    Optional<DiseaseNotificationRecipient> findById(Integer id);

    List<DiseaseNotificationRecipient> findByDefinitionId(Integer definitionId);

    void delete(DiseaseNotificationRecipient recipient);

    void deleteByDefinitionId(Integer definitionId);
}
