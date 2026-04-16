package org.openelisglobal.icd.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.icd.valueholder.IcdVersion;

public interface IcdVersionDAO {

    IcdVersion save(IcdVersion version);

    Optional<IcdVersion> findById(Integer id);

    Optional<IcdVersion> findByCode(String code);

    List<IcdVersion> findAll();

    List<IcdVersion> findAllActive();
}
