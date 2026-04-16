package org.openelisglobal.icd.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.dao.IcdVersionDAO;
import org.openelisglobal.icd.valueholder.IcdVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IcdVersionServiceImpl implements IcdVersionService {

    private static final Logger logger = LoggerFactory.getLogger(IcdVersionServiceImpl.class);

    @Autowired
    private IcdVersionDAO icdVersionDAO;

    @Override
    public IcdVersion save(IcdVersion version) {
        return icdVersionDAO.save(version);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdVersion> findById(Integer id) {
        return icdVersionDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdVersion> findByCode(String code) {
        return icdVersionDAO.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdVersion> findAll() {
        return icdVersionDAO.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdVersion> findAllActive() {
        return icdVersionDAO.findAllActive();
    }

    @Override
    public IcdVersion findOrCreate(String code, String name) {
        Optional<IcdVersion> existing = icdVersionDAO.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        IcdVersion version = new IcdVersion();
        version.setCode(code);
        version.setName(name != null ? name : code);
        version.setActive(true);
        logger.info("Creating new ICD version: {}", code);
        return icdVersionDAO.save(version);
    }
}
