package org.openelisglobal.surveillance.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.icd.service.IcdCodeService;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.openelisglobal.surveillance.dao.DiseaseCaseEvaluationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DiseaseAggregateService {

    @Autowired
    private DiseaseCaseEvaluationDAO evaluationDAO;

    @Autowired
    private IcdCodeService icdCodeService;

    public List<Map<String, Object>> getNationalAggregates(Timestamp startDate, Timestamp endDate) {
        List<Object[]> rows = evaluationDAO.aggregateByIcdCode(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String icdCode = (String) row[0];
            Long count = (Long) row[1];
            String diseaseName = resolveDiseaseName(icdCode);
            Map<String, Object> item = new HashMap<>();
            item.put("icdCode", icdCode);
            item.put("diseaseName", diseaseName);
            item.put("totalCases", count);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getRegionalAggregates(Timestamp startDate, Timestamp endDate) {
        List<Object[]> rows = evaluationDAO.aggregateByIcdCodeAndRegion(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String icdCode = (String) row[0];
            String regionId = (String) row[1];
            Long count = (Long) row[2];
            String diseaseName = resolveDiseaseName(icdCode);
            Map<String, Object> item = new HashMap<>();
            item.put("icdCode", icdCode);
            item.put("diseaseName", diseaseName);
            item.put("region", regionId);
            item.put("totalCases", count);
            result.add(item);
        }
        return result;
    }

    private String resolveDiseaseName(String icdCode) {
        return icdCodeService.findByCode(icdCode).map(IcdCode::getTitle).orElse(icdCode);
    }
}
