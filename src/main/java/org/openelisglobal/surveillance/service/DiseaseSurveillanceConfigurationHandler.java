package org.openelisglobal.surveillance.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration handler for disease surveillance CSV files.
 *
 * Processes two file patterns:
 * - *-definitions.csv : disease_case_definition rows
 * - *-rules.csv       : disease_case_condition rows
 *
 * Definitions CSV format:
 * definitionId,icdCode,version,description,effectiveDate,isActive
 *
 * Rules CSV format:
 * ruleId,icdCode,ruleGroup,operator,testName,resultType,resultValue,numericOperator,numericValue
 */
@Component
public class DiseaseSurveillanceConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private DiseaseCaseDefinitionService definitionService;

    @Autowired
    private DiseaseCaseConditionService conditionService;

    @Override
    public String getDomainName() {
        return "disease-surveillance";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 320;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        if (fileName.endsWith("-definitions.csv") || fileName.contains("definitions")) {
            processDefinitions(inputStream, fileName);
        } else if (fileName.endsWith("-rules.csv") || fileName.contains("rules")) {
            processRules(inputStream, fileName);
        } else {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "Skipping unrecognized disease-surveillance file: " + fileName);
        }
    }

    private void processDefinitions(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) return;

        String[] headers = parseCsvLine(headerLine);
        int icdCodeIndex = findColumnIndex(headers, "icdCode");
        int versionIndex = findColumnIndex(headers, "version");
        int descriptionIndex = findColumnIndex(headers, "description");
        int effectiveDateIndex = findColumnIndex(headers, "effectiveDate");
        int isActiveIndex = findColumnIndex(headers, "isActive");

        if (icdCodeIndex < 0) {
            throw new IllegalArgumentException("Definitions file " + fileName + " must have 'icdCode' column");
        }

        int processed = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            try {
                String[] values = parseCsvLine(line);
                String icdCode = getValueOrEmpty(values, icdCodeIndex);
                if (icdCode.isEmpty()) continue;

                int version = 1;
                String versionStr = getValueOrEmpty(values, versionIndex);
                if (!versionStr.isEmpty()) {
                    try { version = Integer.parseInt(versionStr); } catch (NumberFormatException ignored) {}
                }

                Optional<DiseaseCaseDefinition> existing = definitionService.findActiveByIcdCode(icdCode);
                DiseaseCaseDefinition definition = existing.orElseGet(DiseaseCaseDefinition::new);
                definition.setIcdCode(icdCode);
                definition.setVersion(version);
                definition.setDescription(getValueOrEmpty(values, descriptionIndex));

                String dateStr = getValueOrEmpty(values, effectiveDateIndex);
                if (!dateStr.isEmpty()) {
                    try { definition.setEffectiveDate(Date.valueOf(dateStr)); } catch (IllegalArgumentException ignored) {}
                }

                String isActiveStr = getValueOrEmpty(values, isActiveIndex);
                definition.setActive("Y".equalsIgnoreCase(isActiveStr) || "true".equalsIgnoreCase(isActiveStr)
                        || isActiveStr.isEmpty());

                definitionService.save(definition);
                processed++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processDefinitions",
                        "Error on line in " + fileName + ": " + e.getMessage());
            }
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "processDefinitions",
                "Processed " + processed + " definitions from " + fileName);
    }

    private void processRules(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) return;

        String[] headers = parseCsvLine(headerLine);
        int icdCodeIndex = findColumnIndex(headers, "icdCode");
        int ruleGroupIndex = findColumnIndex(headers, "ruleGroup");
        int operatorIndex = findColumnIndex(headers, "operator");
        int testNameIndex = findColumnIndex(headers, "testName");
        int resultTypeIndex = findColumnIndex(headers, "resultType");
        int resultValueIndex = findColumnIndex(headers, "resultValue");
        int numericOperatorIndex = findColumnIndex(headers, "numericOperator");
        int numericValueIndex = findColumnIndex(headers, "numericValue");

        if (icdCodeIndex < 0 || testNameIndex < 0 || resultTypeIndex < 0) {
            throw new IllegalArgumentException(
                    "Rules file " + fileName + " must have 'icdCode', 'testName', 'resultType' columns");
        }

        int processed = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            try {
                String[] values = parseCsvLine(line);
                String icdCode = getValueOrEmpty(values, icdCodeIndex);
                String testName = getValueOrEmpty(values, testNameIndex);
                String resultType = getValueOrEmpty(values, resultTypeIndex);
                if (icdCode.isEmpty() || testName.isEmpty() || resultType.isEmpty()) continue;

                Optional<DiseaseCaseDefinition> defOpt = definitionService.findActiveByIcdCode(icdCode);
                if (defOpt.isEmpty()) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processRules",
                            "No active definition for ICD code: " + icdCode + " - skipping rule");
                    continue;
                }

                DiseaseCaseDefinition definition = defOpt.get();

                int ruleGroup = 1;
                String ruleGroupStr = getValueOrEmpty(values, ruleGroupIndex);
                if (!ruleGroupStr.isEmpty()) {
                    try { ruleGroup = Integer.parseInt(ruleGroupStr); } catch (NumberFormatException ignored) {}
                }

                DiseaseCaseCondition condition = new DiseaseCaseCondition();
                condition.setDefinition(definition);
                condition.setRuleGroup(ruleGroup);
                condition.setOperator(getValueOrEmpty(values, operatorIndex).isEmpty() ? "AND"
                        : getValueOrEmpty(values, operatorIndex));
                condition.setTestName(testName);
                condition.setResultType(resultType);
                condition.setResultValue(getValueOrEmpty(values, resultValueIndex));

                String numericOperator = getValueOrEmpty(values, numericOperatorIndex);
                if (!numericOperator.isEmpty()) {
                    condition.setNumericOperator(numericOperator);
                }
                String numericValueStr = getValueOrEmpty(values, numericValueIndex);
                if (!numericValueStr.isEmpty()) {
                    try { condition.setNumericValue(new BigDecimal(numericValueStr)); } catch (NumberFormatException ignored) {}
                }

                conditionService.save(condition);
                processed++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processRules",
                        "Error on line in " + fileName + ": " + e.getMessage());
            }
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "processRules",
                "Processed " + processed + " rules from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { values.add(current.toString().trim()); current = new StringBuilder(); }
            else { current.append(c); }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    private int findColumnIndex(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim())) return i;
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) return values[index] != null ? values[index] : "";
        return "";
    }
}
