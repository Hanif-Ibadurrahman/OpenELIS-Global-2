package org.openelisglobal.icd.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.valueholder.IcdVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class IcdVersionDAOImpl implements IcdVersionDAO {

    private static final Logger logger = LoggerFactory.getLogger(IcdVersionDAOImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IcdVersion save(IcdVersion version) {
        if (version.getLastUpdated() == null) {
            version.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        }
        if (version.getId() == null) {
            entityManager.persist(version);
            return version;
        } else {
            return entityManager.merge(version);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdVersion> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(IcdVersion.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdVersion> findByCode(String code) {
        try {
            List<IcdVersion> results = entityManager
                    .createQuery("FROM IcdVersion v WHERE v.code = :code", IcdVersion.class)
                    .setParameter("code", code).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding IcdVersion by code: {}", code, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdVersion> findAll() {
        return entityManager.createQuery("FROM IcdVersion v ORDER BY v.code", IcdVersion.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdVersion> findAllActive() {
        return entityManager.createQuery("FROM IcdVersion v WHERE v.isActive = true ORDER BY v.code", IcdVersion.class)
                .getResultList();
    }
}
