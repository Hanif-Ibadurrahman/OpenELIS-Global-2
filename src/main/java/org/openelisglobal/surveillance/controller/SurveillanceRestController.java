package org.openelisglobal.surveillance.controller;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.icd.service.IcdCodeService;
import org.openelisglobal.icd.valueholder.IcdCode;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.surveillance.service.DiseaseAggregateService;
import org.openelisglobal.surveillance.service.DiseaseCaseConditionService;
import org.openelisglobal.surveillance.service.DiseaseCaseDefinitionService;
import org.openelisglobal.surveillance.service.DiseaseNotificationRecipientService;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/surveillance")
public class SurveillanceRestController {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private IcdCodeService icdCodeService;

    @Autowired
    private DiseaseCaseDefinitionService definitionService;

    @Autowired
    private DiseaseCaseConditionService conditionService;

    @Autowired
    private DiseaseAggregateService aggregateService;

    @Autowired
    private DiseaseNotificationRecipientService recipientService;

    @Autowired
    private PersonService personService;

    // ── ICD Codes ──────────────────────────────────────────────────────────────

    @GetMapping("/icd-codes")
    public ResponseEntity<List<Map<String, Object>>> getIcdCodes(
            @RequestParam(required = false) String q) {
        try {
            List<IcdCode> codes = (q != null && !q.isBlank())
                    ? icdCodeService.searchByTitle(q)
                    : icdCodeService.findAllActive();
            List<Map<String, Object>> result = new ArrayList<>();
            for (IcdCode c : codes) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("code", c.getCode());
                item.put("title", c.getTitle());
                item.put("status", c.getStatus());
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getIcdCodes", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Disease Aggregates ─────────────────────────────────────────────────────

    @GetMapping("/disease-aggregates")
    public ResponseEntity<Map<String, Object>> getDiseaseAggregates(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String icdCode) {
        try {
            Timestamp start = parseTimestamp(startDate);
            Timestamp end = parseTimestamp(endDate);

            List<Map<String, Object>> national = aggregateService.getNationalAggregates(start, end);
            List<Map<String, Object>> regional = aggregateService.getRegionalAggregates(start, end);

            if (icdCode != null && !icdCode.isBlank()) {
                national = national.stream().filter(m -> icdCode.equals(m.get("icdCode"))).toList();
                regional = regional.stream().filter(m -> icdCode.equals(m.get("icdCode"))).toList();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("national", national);
            response.put("regional", regional);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getDiseaseAggregates", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Case Definitions CRUD ─────────────────────────────────────────────────

    @GetMapping("/case-definitions")
    public ResponseEntity<List<Map<String, Object>>> getCaseDefinitions(
            @RequestParam(required = false) String icdCode) {
        try {
            List<DiseaseCaseDefinition> defs = (icdCode != null && !icdCode.isBlank())
                    ? definitionService.findByIcdCode(icdCode)
                    : definitionService.findAll();
            return ResponseEntity.ok(defs.stream().map(this::definitionToMap).toList());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getCaseDefinitions", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/case-definitions/{id}")
    public ResponseEntity<Map<String, Object>> getCaseDefinition(@PathVariable Integer id) {
        return definitionService.findById(id)
                .map(d -> ResponseEntity.ok(definitionToMap(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/case-definitions")
    public ResponseEntity<Map<String, Object>> createCaseDefinition(
            @RequestBody Map<String, Object> body) {
        try {
            DiseaseCaseDefinition def = mapToDefinition(null, body);
            def = definitionService.save(def);
            return ResponseEntity.status(HttpStatus.CREATED).body(definitionToMap(def));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createCaseDefinition", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/case-definitions/{id}")
    public ResponseEntity<Map<String, Object>> updateCaseDefinition(@PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        try {
            Optional<DiseaseCaseDefinition> existing = definitionService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();
            DiseaseCaseDefinition def = mapToDefinition(existing.get(), body);
            def = definitionService.save(def);
            return ResponseEntity.ok(definitionToMap(def));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateCaseDefinition", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/case-definitions/{id}")
    public ResponseEntity<Void> deleteCaseDefinition(@PathVariable Integer id) {
        try {
            definitionService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteCaseDefinition", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Case Rules CRUD ────────────────────────────────────────────────────────

    @GetMapping("/case-rules")
    public ResponseEntity<List<Map<String, Object>>> getCaseRules(
            @RequestParam(required = false) Integer definitionId) {
        try {
            List<DiseaseCaseCondition> rules = (definitionId != null)
                    ? conditionService.findByDefinitionId(definitionId)
                    : new ArrayList<>();
            return ResponseEntity.ok(rules.stream().map(this::conditionToMap).toList());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getCaseRules", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/case-rules")
    public ResponseEntity<Map<String, Object>> createCaseRule(@RequestBody Map<String, Object> body) {
        try {
            DiseaseCaseCondition condition = mapToCondition(null, body);
            if (condition.getDefinition() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            condition = conditionService.save(condition);
            return ResponseEntity.status(HttpStatus.CREATED).body(conditionToMap(condition));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createCaseRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/case-rules/{id}")
    public ResponseEntity<Map<String, Object>> updateCaseRule(@PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        try {
            Optional<DiseaseCaseCondition> existing = conditionService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();
            DiseaseCaseCondition condition = mapToCondition(existing.get(), body);
            condition = conditionService.save(condition);
            return ResponseEntity.ok(conditionToMap(condition));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateCaseRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/case-rules/{id}")
    public ResponseEntity<Void> deleteCaseRule(@PathVariable Integer id) {
        try {
            conditionService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteCaseRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Notification Recipients ────────────────────────────────────────────────

    @GetMapping("/persons")
    public ResponseEntity<List<Map<String, Object>>> getPersons() {
        try {
            List<Person> persons = personService.getAllPersons();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Person p : persons) {
                if ((p.getEmail() == null || p.getEmail().isBlank())
                        && (p.getCellPhone() == null || p.getCellPhone().isBlank())
                        && (p.getWorkPhone() == null || p.getWorkPhone().isBlank())) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                String first = p.getFirstName() != null ? p.getFirstName() : "";
                String last = p.getLastName() != null ? p.getLastName() : "";
                m.put("name", (first + " " + last).trim());
                m.put("email", p.getEmail());
                m.put("workPhone", p.getWorkPhone());
                m.put("cellPhone", p.getCellPhone());
                result.add(m);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getPersons", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/case-definitions/{id}/recipients")
    public ResponseEntity<List<Map<String, Object>>> getRecipients(@PathVariable Integer id) {
        try {
            List<DiseaseNotificationRecipient> recipients = recipientService.findByDefinitionId(id);
            return ResponseEntity.ok(recipients.stream().map(this::recipientToMap).toList());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getRecipients", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/case-definitions/{id}/recipients")
    public ResponseEntity<List<Map<String, Object>>> replaceRecipients(@PathVariable Integer id,
            @RequestBody List<Map<String, Object>> body) {
        try {
            if (definitionService.findById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<DiseaseNotificationRecipient> recipients = new ArrayList<>();
            for (Map<String, Object> item : body) {
                DiseaseNotificationRecipient r = new DiseaseNotificationRecipient();
                r.setPersonId(String.valueOf(item.get("personId")));
                Object type = item.get("notificationType");
                r.setNotificationType(type != null ? String.valueOf(type) : "all");
                recipients.add(r);
            }
            List<DiseaseNotificationRecipient> saved = recipientService.replaceForDefinition(id, recipients);
            return ResponseEntity.ok(saved.stream().map(this::recipientToMap).toList());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "replaceRecipients", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private Map<String, Object> recipientToMap(DiseaseNotificationRecipient r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("definitionId", r.getDefinition() != null ? r.getDefinition().getId() : null);
        m.put("personId", r.getPersonId());
        m.put("notificationType", r.getNotificationType());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        try {
            Person p = personService.getPersonById(r.getPersonId());
            if (p != null) {
                String first = p.getFirstName() != null ? p.getFirstName() : "";
                String last = p.getLastName() != null ? p.getLastName() : "";
                m.put("name", (first + " " + last).trim());
                m.put("email", p.getEmail());
                m.put("workPhone", p.getWorkPhone());
                m.put("cellPhone", p.getCellPhone());
            }
        } catch (Exception ignored) {}
        return m;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> definitionToMap(DiseaseCaseDefinition d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("icdCode", d.getIcdCode());
        m.put("version", d.getVersion());
        m.put("description", d.getDescription());
        m.put("effectiveDate", d.getEffectiveDate() != null ? d.getEffectiveDate().toString() : null);
        m.put("isActive", d.isActive());
        m.put("timeRange", d.getTimeRange());
        m.put("thresholdType", d.getThresholdType());
        m.put("thresholdValue", d.getThresholdValue());
        m.put("nextNotification", d.getNextNotification() != null ? d.getNextNotification().toString() : null);
        String diseaseName = icdCodeService.findByCode(d.getIcdCode())
                .map(IcdCode::getTitle).orElse(d.getIcdCode());
        m.put("diseaseName", diseaseName);
        return m;
    }

    private DiseaseCaseDefinition mapToDefinition(DiseaseCaseDefinition existing, Map<String, Object> body) {
        DiseaseCaseDefinition def = existing != null ? existing : new DiseaseCaseDefinition();
        if (body.containsKey("icdCode")) def.setIcdCode((String) body.get("icdCode"));
        if (body.containsKey("version") && body.get("version") != null) {
            def.setVersion(((Number) body.get("version")).intValue());
        }
        if (body.containsKey("description")) def.setDescription((String) body.get("description"));
        if (body.containsKey("effectiveDate") && body.get("effectiveDate") != null) {
            try { def.setEffectiveDate(java.sql.Date.valueOf((String) body.get("effectiveDate"))); } catch (Exception ignored) {}
        }
        if (body.containsKey("isActive")) {
            Object active = body.get("isActive");
            def.setActive(active instanceof Boolean ? (Boolean) active : "true".equalsIgnoreCase(String.valueOf(active)));
        }
        if (body.containsKey("timeRange")) {
            Object v = body.get("timeRange");
            def.setTimeRange(v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v));
        }
        if (body.containsKey("thresholdType")) {
            Object v = body.get("thresholdType");
            def.setThresholdType(v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v));
        }
        if (body.containsKey("thresholdValue")) {
            Object v = body.get("thresholdValue");
            if (v == null || String.valueOf(v).isBlank()) {
                def.setThresholdValue(null);
            } else {
                try { def.setThresholdValue(new java.math.BigDecimal(String.valueOf(v))); } catch (Exception ignored) {}
            }
        }
        return def;
    }

    private Map<String, Object> conditionToMap(DiseaseCaseCondition c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("definitionId", c.getDefinition() != null ? c.getDefinition().getId() : null);
        m.put("ruleGroup", c.getRuleGroup());
        m.put("operator", c.getOperator());
        m.put("testName", c.getTestName());
        m.put("resultType", c.getResultType());
        m.put("resultValue", c.getResultValue());
        m.put("numericOperator", c.getNumericOperator());
        m.put("numericValue", c.getNumericValue());
        return m;
    }

    private DiseaseCaseCondition mapToCondition(DiseaseCaseCondition existing, Map<String, Object> body) {
        DiseaseCaseCondition condition = existing != null ? existing : new DiseaseCaseCondition();
        if (body.containsKey("definitionId") && body.get("definitionId") != null) {
            Integer defId = ((Number) body.get("definitionId")).intValue();
            definitionService.findById(defId).ifPresent(condition::setDefinition);
        }
        if (body.containsKey("ruleGroup") && body.get("ruleGroup") != null) {
            condition.setRuleGroup(((Number) body.get("ruleGroup")).intValue());
        }
        if (body.containsKey("operator")) condition.setOperator((String) body.get("operator"));
        if (body.containsKey("testName")) condition.setTestName((String) body.get("testName"));
        if (body.containsKey("resultType")) condition.setResultType((String) body.get("resultType"));
        if (body.containsKey("resultValue")) condition.setResultValue((String) body.get("resultValue"));
        if (body.containsKey("numericOperator")) condition.setNumericOperator((String) body.get("numericOperator"));
        if (body.containsKey("numericValue") && body.get("numericValue") != null) {
            condition.setNumericValue(new java.math.BigDecimal(String.valueOf(body.get("numericValue"))));
        }
        return condition;
    }

    private Timestamp parseTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return new Timestamp(DATE_FORMAT.parse(dateStr).getTime());
        } catch (Exception e) {
            return null;
        }
    }
}
