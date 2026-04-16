package org.openelisglobal.icd.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.dao.IcdCodeDAO;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.openelisglobal.icd.valueholder.IcdVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IcdCodeServiceImpl implements IcdCodeService {

    private static final Logger logger = LoggerFactory.getLogger(IcdCodeServiceImpl.class);

    @Autowired
    private IcdCodeDAO icdCodeDAO;

    @Override
    public IcdCode save(IcdCode code) {
        return icdCodeDAO.save(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findById(Integer id) {
        return icdCodeDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findByVersionAndCode(Integer versionId, String code) {
        return icdCodeDAO.findByVersionAndCode(versionId, code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IcdCode> findByCode(String code) {
        return icdCodeDAO.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findByVersionId(Integer versionId) {
        return icdCodeDAO.findByVersionId(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findActiveByVersionId(Integer versionId) {
        return icdCodeDAO.findActiveByVersionId(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> findAllActive() {
        return icdCodeDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IcdCode> searchByTitle(String titleFragment) {
        return icdCodeDAO.searchByTitle(titleFragment);
    }

    @Override
    public IcdCode findOrCreate(IcdVersion version, String code, String title, IcdCode parent, Integer level) {
        Optional<IcdCode> existing = icdCodeDAO.findByVersionAndCode(version.getId(), code);
        if (existing.isPresent()) {
            IcdCode icd = existing.get();
            icd.setTitle(title);
            return icdCodeDAO.save(icd);
        }
        IcdCode icdCode = new IcdCode();
        icdCode.setVersion(version);
        icdCode.setCode(code);
        icdCode.setTitle(title);
        icdCode.setParent(parent);
        icdCode.setLevel(level);
        icdCode.setStatus("active");
        icdCode.setLeaf(true);
        if (parent != null && parent.isLeaf()) {
            parent.setLeaf(false);
            icdCodeDAO.save(parent);
        }
        logger.debug("Creating ICD code: {} - {}", code, title);
        return icdCodeDAO.save(icdCode);
    }
}
