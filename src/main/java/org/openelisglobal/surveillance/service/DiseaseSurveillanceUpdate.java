package org.openelisglobal.surveillance.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;

/**
 * Triggers disease surveillance evaluation when results are confirmed (signed).
 * Only evaluates samples where results have been signed (e-signature confirmation).
 */
public class DiseaseSurveillanceUpdate implements IResultUpdate {

    private static final boolean ENABLED = isDiseaseSurveillanceEnabled();

    @Override
    public void transactionalUpdate(IResultSaveService resultService) throws LIMSRuntimeException {
        if (!ENABLED) {
            return;
        }

        try {
            DiseaseEvaluationService evaluationService = SpringContext.getBean(DiseaseEvaluationService.class);

            // Collect all samples with signed results
            Set<String> evaluatedSamples = new HashSet<>();
            Map<String, Map<String, String>> sampleTestResults = new HashMap<>();

            // Process new results with signatures
            for (ResultSet resultSet : resultService.getNewResults()) {
                if (resultSet.signature != null && resultSet.signature.getId() != null) {
                    String sampleId = resultSet.sample.getId();
                    if (!evaluatedSamples.contains(sampleId)) {
                        evaluatedSamples.add(sampleId);
                        collectTestResults(resultSet, sampleTestResults);
                    }
                }
            }

            // Process modified results with signatures
            for (ResultSet resultSet : resultService.getModifiedResults()) {
                if (resultSet.signature != null && resultSet.signature.getId() != null) {
                    String sampleId = resultSet.sample.getId();
                    if (!evaluatedSamples.contains(sampleId)) {
                        evaluatedSamples.add(sampleId);
                        collectTestResults(resultSet, sampleTestResults);
                    }
                }
            }

            // Evaluate each sample with signed results
            for (String sampleId : evaluatedSamples) {
                Map<String, String> testResults = sampleTestResults.get(sampleId);
                if (testResults != null && !testResults.isEmpty()) {
                    ResultSet resultSet = resultSetForResultSet(resultService, sampleId);
                    if (resultSet != null) {
                        String patientId = resultSet.patient != null ? resultSet.patient.getId() : "";
                        // Region is not directly available on Sample or Patient, using empty string
                        String regionId = "";

                        evaluationService.evaluateSample(sampleId, patientId, regionId, testResults);
                        LogEvent.logInfo(this.getClass().getSimpleName(), "transactionalUpdate",
                                "Disease surveillance evaluated for sample " + sampleId);
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "transactionalUpdate",
                    "Error in disease surveillance evaluation: " + e.getMessage());
            // Don't throw - allow transaction to complete
        }
    }

    @Override
    public void postTransactionalCommitUpdate(IResultSaveService resultService) {
        // No-op for post-commit processing
    }

    private void collectTestResults(ResultSet resultSet, Map<String, Map<String, String>> sampleTestResults) {
        String sampleId = resultSet.sample.getId();
        sampleTestResults.computeIfAbsent(sampleId, k -> new HashMap<>());

        // Get test name from Analysis
        String testName = "";
        if (resultSet.result.getAnalysis() != null && resultSet.result.getAnalysis().getTest() != null) {
            testName = resultSet.result.getAnalysis().getTest().getName();
        }
        String resultValue = resultSet.result.getValue() != null ? resultSet.result.getValue() : "";

        if (!testName.isEmpty() && !resultValue.isEmpty()) {
            sampleTestResults.get(sampleId).put(testName, resultValue);
        }
    }

    private ResultSet resultSetForResultSet(IResultSaveService resultService, String sampleId) {
        for (ResultSet resultSet : resultService.getNewResults()) {
            if (resultSet.sample.getId().equals(sampleId)) {
                return resultSet;
            }
        }
        for (ResultSet resultSet : resultService.getModifiedResults()) {
            if (resultSet.sample.getId().equals(sampleId)) {
                return resultSet;
            }
        }
        return null;
    }

    private static boolean isDiseaseSurveillanceEnabled() {
        String enabled = ConfigurationProperties.getInstance().getPropertyValue(Property.DISEASE_SURVEILLANCE);
        return "true".equalsIgnoreCase(enabled) || "enable".equalsIgnoreCase(enabled);
    }
}
