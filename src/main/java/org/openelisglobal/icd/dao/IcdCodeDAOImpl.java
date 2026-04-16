package org.openelisglobal.icd.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class IcdCodeDAOImpl implements IcdCodeDAO {

    private static final Logger logger = LoggerFactory.getLogger(IcdCodeDAOImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IcdCode save(IcdCode icdCode) {
        if (icdCode.getLastUpdated() == null) {
            icdCode.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        }
        if (icdCode.getId() == null) {
            entityManager.persist(icdCode);
            return icdCode;
        } else {
            return entityManager.merge(icdCode);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(IcdCode.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findByVersionAndCode(Integer versionId, String code) {
        try {
            List<IcdCode> results = entityManager
                    .createQuery("FROM IcdCode c WHERE c.version.id = :versionId AND c.code = :code", IcdCode.class)
                    .setParameter("versionId", versionId).setParameter("code", code).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding IcdCode by version and code", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findByCode(String code) {
        try {
            List<IcdCode> results = entityManager
                    .createQuery("FROM IcdCode c WHERE c.code = :code AND c.status = 'active' ORDER BY c.id",
                            IcdCode.class)
                    .setParameter("code", code).setMaxResults(1).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding IcdCode by code: {}", code, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findByVersionId(Integer versionId) {
        return entityManager
                .createQuery("FROM IcdCode c WHERE c.version.id = :versionId ORDER BY c.code", IcdCode.class)
                .setParameter("versionId", versionId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findActiveByVersionId(Integer versionId) {
        return entityManager
                .createQuery(
                        "FROM IcdCode c WHERE c.version.id = :versionId AND c.status = 'active' ORDER BY c.code",
                        IcdCode.class)
                .setParameter("versionId", versionId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findAllActive() {
        return entityManager
                .createQuery("FROM IcdCode c WHERE c.status = 'active' ORDER BY c.code", IcdCode.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> searchByTitle(String titleFragment) {
        return entityManager
                .createQuery(
                        "FROM IcdCode c WHERE c.status = 'active' AND LOWER(c.title) LIKE :pattern ORDER BY c.code",
                        IcdCode.class)
                .setParameter("pattern", "%" + titleFragment.toLowerCase() + "%").setMaxResults(50).getResultList();
    }
}
