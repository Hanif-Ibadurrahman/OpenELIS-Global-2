package org.openelisglobal.surveillance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DiseaseNotificationRecipientDAOImpl implements DiseaseNotificationRecipientDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DiseaseNotificationRecipient save(DiseaseNotificationRecipient recipient) {
        if (recipient.getCreatedAt() == null) {
            recipient.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        if (recipient.getId() == null) {
            entityManager.persist(recipient);
            return recipient;
        } else {
            return entityManager.merge(recipient);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiseaseNotificationRecipient> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(DiseaseNotificationRecipient.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseNotificationRecipient> findByDefinitionId(Integer definitionId) {
        return entityManager
                .createQuery(
                        "FROM DiseaseNotificationRecipient r WHERE r.definition.id = :defId ORDER BY r.id ASC",
                        DiseaseNotificationRecipient.class)
                .setParameter("defId", definitionId).getResultList();
    }

    @Override
    public void delete(DiseaseNotificationRecipient recipient) {
        entityManager.remove(entityManager.contains(recipient) ? recipient : entityManager.merge(recipient));
    }

    @Override
    public void deleteByDefinitionId(Integer definitionId) {
        entityManager.createQuery("DELETE FROM DiseaseNotificationRecipient r WHERE r.definition.id = :defId")
                .setParameter("defId", definitionId).executeUpdate();
    }
}
