package org.openelisglobal.icd.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.openelisglobal.icd.valueholder.IcdVersion;

public interface IcdCodeService {

    IcdCode save(IcdCode code);

    Optional<IcdCode> findById(Integer id);

    Optional<IcdCode> findByVersionAndCode(Integer versionId, String code);

    Optional<IcdCode> findByCode(String code);

    List<IcdCode> findByVersionId(Integer versionId);

    List<IcdCode> findActiveByVersionId(Integer versionId);

    List<IcdCode> findAllActive();

    List<IcdCode> searchByTitle(String titleFragment);

    IcdCode findOrCreate(IcdVersion version, String code, String title, IcdCode parent, Integer level);
}
