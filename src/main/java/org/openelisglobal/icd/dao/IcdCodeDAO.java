package org.openelisglobal.icd.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.valueholder.IcdCode;

public interface IcdCodeDAO {

    IcdCode save(IcdCode code);

    Optional<IcdCode> findById(Integer id);

    Optional<IcdCode> findByVersionAndCode(Integer versionId, String code);

    Optional<IcdCode> findByCode(String code);

    List<IcdCode> findByVersionId(Integer versionId);

    List<IcdCode> findActiveByVersionId(Integer versionId);

    List<IcdCode> findAllActive();

    List<IcdCode> searchByTitle(String titleFragment);
}
