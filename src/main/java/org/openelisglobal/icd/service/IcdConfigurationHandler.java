package org.openelisglobal.icd.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.openelisglobal.icd.valueholder.IcdVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration handler for loading ICD codes from CSV files.
 *
 * Supports both ICD-10 and ICD-11 formats.
 *
 * Expected CSV format:
 * CODE,TITLE,DESCRIPTION,VERSION,STATUS
 * A00,Cholera,Cholera due to Vibrio cholerae,ICD10_2010,active
 * A00.0,Cholera due to vibrio cholerae 01 biovar cholerae,,ICD10_2010,active
 */
@Component
public class IcdConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private IcdVersionService icdVersionService;

    @Autowired
    private IcdCodeService icdCodeService;

    @Override
    public String getDomainName() {
        return "icd";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 315;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "ICD file " + fileName + " is empty - skipping");
            return;
        }

        String[] headers = parseCsvLine(headerLine);
        int codeIndex = findColumnIndex(headers, "CODE");
        int titleIndex = findColumnIndex(headers, "TITLE");
        int descriptionIndex = findColumnIndex(headers, "DESCRIPTION");
        int versionIndex = findColumnIndex(headers, "VERSION");
        int statusIndex = findColumnIndex(headers, "STATUS");

        if (codeIndex < 0 || titleIndex < 0 || versionIndex < 0) {
            throw new IllegalArgumentException(
                    "ICD file " + fileName + " must have CODE, TITLE, VERSION columns");
        }

        Map<String, IcdVersion> versionCache = new HashMap<>();
        Map<String, IcdCode> codeCache = new HashMap<>();

        List<String[]> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            rows.add(parseCsvLine(line));
        }

        int processed = 0;
        for (String[] values : rows) {
            try {
                String code = getValueOrEmpty(values, codeIndex);
                String title = getValueOrEmpty(values, titleIndex);
                String description = getValueOrEmpty(values, descriptionIndex);
                String versionCode = getValueOrEmpty(values, versionIndex);
                String status = getValueOrEmpty(values, statusIndex);

                if (code.isEmpty() || title.isEmpty() || versionCode.isEmpty()) {
                    continue;
                }
                if (status.isEmpty()) {
                    status = "active";
                }

                IcdVersion version = versionCache.computeIfAbsent(versionCode,
                        vc -> icdVersionService.findOrCreate(vc, deriveVersionName(vc)));

                IcdCode parent = resolveParent(code, versionCode, codeCache);
                int level = computeLevel(code);

                IcdCode icdCode = icdCodeService.findOrCreate(version, code, title, parent, level);
                if (!status.equals(icdCode.getStatus())) {
                    icdCode.setStatus(status);
                    icdCodeService.save(icdCode);
                }
                if (description != null && !description.isEmpty()) {
                    icdCode.setDescription(description);
                    icdCodeService.save(icdCode);
                }
                codeCache.put(versionCode + ":" + code, icdCode);
                processed++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing ICD code row: " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Processed " + processed + " ICD codes from " + fileName);
    }

    private String deriveVersionName(String versionCode) {
        if (versionCode.startsWith("ICD10")) {
            return "ICD-10 " + versionCode.replace("ICD10_", "") + " Edition";
        } else if (versionCode.startsWith("ICD11")) {
            return "ICD-11 " + versionCode.replace("ICD11_", "") + " Edition";
        }
        return versionCode;
    }

    private IcdCode resolveParent(String code, String versionCode, Map<String, IcdCode> codeCache) {
        String parentCode = deriveParentCode(code);
        if (parentCode == null) {
            return null;
        }
        return codeCache.get(versionCode + ":" + parentCode);
    }

    private String deriveParentCode(String code) {
        if (code == null || code.length() < 2) {
            return null;
        }
        int dotIndex = code.lastIndexOf('.');
        if (dotIndex > 0) {
            String beforeDot = code.substring(0, dotIndex);
            String afterDot = code.substring(dotIndex + 1);
            if (afterDot.length() > 1) {
                return beforeDot + "." + afterDot.substring(0, afterDot.length() - 1);
            }
            return beforeDot;
        }
        if (code.length() > 3 && Character.isLetter(code.charAt(code.length() - 1))) {
            return code.substring(0, code.length() - 1);
        }
        return null;
    }

    private int computeLevel(String code) {
        if (code == null) return 0;
        int dotIndex = code.indexOf('.');
        if (dotIndex < 0) return 1;
        return 2 + (code.length() - dotIndex - 2);
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
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
        if (index >= 0 && index < values.length) {
            return values[index] != null ? values[index] : "";
        }
        return "";
    }
}
