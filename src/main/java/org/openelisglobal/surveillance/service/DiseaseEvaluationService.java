package org.openelisglobal.surveillance.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.service.sender.EmailNotificationSender;
import org.openelisglobal.notification.service.sender.SMSNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.surveillance.dao.DiseaseCaseDefinitionDAO;
import org.openelisglobal.surveillance.dao.DiseaseCaseEvaluationDAO;
import org.openelisglobal.surveillance.dao.DiseaseNotificationRecipientDAO;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseCondition;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseDefinition;
import org.openelisglobal.surveillance.valueholder.DiseaseCaseEvaluation;
import org.openelisglobal.surveillance.valueholder.DiseaseNotificationRecipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates active disease case definitions against provided test results.
 * Called after a sample's results are confirmed.
 */
@Service
@Transactional
public class DiseaseEvaluationService {

    @Autowired
    private DiseaseCaseDefinitionService definitionService;

    @Autowired
    private DiseaseCaseConditionService conditionService;

    @Autowired
    private DiseaseCaseEvaluationDAO evaluationDAO;

    @Autowired
    private DiseaseCaseDefinitionDAO definitionDAO;

    @Autowired
    private DiseaseNotificationRecipientDAO recipientDAO;

    @Autowired
    private PersonService personService;

    @Autowired
    private EmailNotificationSender emailNotificationSender;

    @Autowired
    private SMSNotificationSender smsNotificationSender;

    /**
     * Evaluates all active disease case definitions against the provided test
     * results map. Stores a DiseaseCaseEvaluation record for each matched disease.
     *
     * @param sampleId      the sample being evaluated
     * @param patientId     patient ID
     * @param regionId      region ID for aggregate queries
     * @param testResults   map of testName -> resultValue (String)
     */
    public void evaluateSample(String sampleId, String patientId, String regionId,
            Map<String, String> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return;
        }

        List<DiseaseCaseDefinition> activeDefinitions = definitionService.findAllActive();
        for (DiseaseCaseDefinition definition : activeDefinitions) {
            try {
                List<DiseaseCaseCondition> conditions = conditionService
                        .findByDefinitionId(definition.getId());
                if (conditions.isEmpty()) {
                    continue;
                }

                EvaluationResult eval = evaluateDefinition(definition, conditions, testResults);
                if (eval.isMatched()) {
                    DiseaseCaseEvaluation record = new DiseaseCaseEvaluation();
                    record.setIcdCode(definition.getIcdCode());
                    record.setSampleId(sampleId);
                    record.setPatientId(patientId);
                    record.setRegionId(regionId);
                    record.setConfirmed(true);
                    record.setDefinitionId(definition.getId());
                    record.setEvaluationDate(new Timestamp(System.currentTimeMillis()));
                    record.setMatchedConditions(String.join(", ", eval.getMatchedConditionIds()));
                    evaluationDAO.save(record);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "evaluateSample",
                            "Disease matched: " + definition.getIcdCode() + " for sample " + sampleId);

                    checkThresholdAndNotify(definition);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "evaluateSample",
                        "Error evaluating definition " + definition.getId() + ": " + e.getMessage());
            }
        }
    }

    private EvaluationResult evaluateDefinition(DiseaseCaseDefinition definition,
            List<DiseaseCaseCondition> conditions, Map<String, String> testResults) {

        Map<Integer, List<DiseaseCaseCondition>> byGroup = conditions.stream()
                .collect(Collectors.groupingBy(DiseaseCaseCondition::getRuleGroup));

        List<String> matchedConditionIds = new ArrayList<>();
        boolean allGroupsMatched = true;

        for (Map.Entry<Integer, List<DiseaseCaseCondition>> groupEntry : byGroup.entrySet()) {
            List<DiseaseCaseCondition> groupConditions = groupEntry.getValue();
            boolean groupMatched = evaluateGroup(groupConditions, testResults, matchedConditionIds);
            if (!groupMatched) {
                allGroupsMatched = false;
                break;
            }
        }

        return new EvaluationResult(allGroupsMatched, matchedConditionIds);
    }

    private boolean evaluateGroup(List<DiseaseCaseCondition> groupConditions,
            Map<String, String> testResults, List<String> matchedIds) {

        if (groupConditions.isEmpty()) {
            return false;
        }

        // Sort conditions by ID to ensure consistent left-to-right evaluation
        groupConditions.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        // First condition has no operator - it's the starting value
        DiseaseCaseCondition first = groupConditions.get(0);
        boolean result = evaluateCondition(first, testResults);
        if (result) {
            matchedIds.add(String.valueOf(first.getId()));
        }

        // Subsequent conditions use their operator to combine with the previous result
        for (int i = 1; i < groupConditions.size(); i++) {
            DiseaseCaseCondition condition = groupConditions.get(i);
            boolean conditionMet = evaluateCondition(condition, testResults);
            String op = condition.getOperator();

            if ("OR".equalsIgnoreCase(op)) {
                result = result || conditionMet;
            } else if ("AND".equalsIgnoreCase(op)) {
                result = result && conditionMet;
            } else {
                // Default to AND if operator is null/empty
                result = result && conditionMet;
            }

            if (conditionMet) {
                matchedIds.add(String.valueOf(condition.getId()));
            }
        }

        return result;
    }

    private boolean evaluateCondition(DiseaseCaseCondition condition, Map<String, String> testResults) {
        String testValue = testResults.get(condition.getTestName());
        if (testValue == null) {
            return false;
        }

        if ("D".equalsIgnoreCase(condition.getResultType())) {
            return condition.getResultValue() != null
                    && condition.getResultValue().equalsIgnoreCase(testValue.trim());
        } else if ("N".equalsIgnoreCase(condition.getResultType())) {
            return evaluateNumeric(testValue, condition.getNumericOperator(), condition.getNumericValue());
        }
        return false;
    }

    private boolean evaluateNumeric(String testValue, String numericOperator, BigDecimal threshold) {
        if (numericOperator == null || threshold == null) return false;
        try {
            BigDecimal value = new BigDecimal(testValue.trim());
            return switch (numericOperator.toUpperCase()) {
                case "EQ" -> value.compareTo(threshold) == 0;
                case "LT" -> value.compareTo(threshold) < 0;
                case "GT" -> value.compareTo(threshold) > 0;
                case "LTE" -> value.compareTo(threshold) <= 0;
                case "GTE" -> value.compareTo(threshold) >= 0;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks the threshold conditions for the matched definition and sends
     * notifications to recipients if the threshold is exceeded and not in
     * cooldown period.
     */
    private void checkThresholdAndNotify(DiseaseCaseDefinition definition) {
        try {
            String thresholdType = definition.getThresholdType();
            if (thresholdType == null || thresholdType.isBlank()) {
                return;
            }

            // Cooldown check: skip if next_notification is set and still in future
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp nextNotification = definition.getNextNotification();
            if (nextNotification != null && nextNotification.compareTo(now) >= 0) {
                return;
            }

            boolean shouldNotify = false;

            switch (thresholdType.toLowerCase()) {
                case "single_case":
                    shouldNotify = true;
                    break;
                case "count":
                    shouldNotify = isCountThresholdExceeded(definition);
                    break;
                case "rate":
                case "cluster":
                case "trend":
                    LogEvent.logInfo(this.getClass().getSimpleName(), "checkThresholdAndNotify",
                            "Threshold type '" + thresholdType + "' not yet implemented for definition "
                                    + definition.getId());
                    break;
                default:
                    break;
            }

            if (shouldNotify) {
                sendNotifications(definition);
                updateNextNotification(definition, now);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "checkThresholdAndNotify",
                    "Error checking threshold for definition " + definition.getId() + ": " + e.getMessage());
        }
    }

    private boolean isCountThresholdExceeded(DiseaseCaseDefinition definition) {
        BigDecimal threshold = definition.getThresholdValue();
        if (threshold == null) return false;
        Timestamp since = computeRangeStart(definition.getTimeRange());
        long count = evaluationDAO.countByDefinitionIdSince(definition.getId(), since);
        return BigDecimal.valueOf(count).compareTo(threshold) >= 0;
    }

    private Timestamp computeRangeStart(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) return null;
        Calendar cal = Calendar.getInstance();
        switch (timeRange.toLowerCase()) {
            case "daily":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "weekly":
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "monthly":
                cal.add(Calendar.MONTH, -1);
                break;
            default:
                return null;
        }
        return new Timestamp(cal.getTimeInMillis());
    }

    private void updateNextNotification(DiseaseCaseDefinition definition, Timestamp now) {
        String timeRange = definition.getTimeRange();
        if (timeRange == null || timeRange.isBlank()) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now.getTime());
        switch (timeRange.toLowerCase()) {
            case "daily":
                cal.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "weekly":
                cal.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case "monthly":
                cal.add(Calendar.MONTH, 1);
                break;
            default:
                return;
        }
        definition.setNextNotification(new Timestamp(cal.getTimeInMillis()));
        definitionDAO.save(definition);
    }

    private void sendNotifications(DiseaseCaseDefinition definition) {
        List<DiseaseNotificationRecipient> recipients = recipientDAO.findByDefinitionId(definition.getId());
        if (recipients == null || recipients.isEmpty()) return;

        String subject = "Disease Surveillance Alert: " + definition.getIcdCode();
        String message = "Threshold exceeded for disease case definition " + definition.getIcdCode()
                + (definition.getDescription() != null ? " - " + definition.getDescription() : "")
                + ". Threshold type: " + definition.getThresholdType()
                + ", value: " + definition.getThresholdValue()
                + ", time range: " + definition.getTimeRange() + ".";

        NotificationPayload payload = new SimpleNotificationPayload(subject, message);

        for (DiseaseNotificationRecipient recipient : recipients) {
            try {
                Person person = personService.getPersonById(recipient.getPersonId());
                if (person == null) continue;
                String type = recipient.getNotificationType();
                if (type == null) continue;

                boolean wantEmail = "email".equalsIgnoreCase(type) || "all".equalsIgnoreCase(type);
                boolean wantSms = "sms".equalsIgnoreCase(type) || "all".equalsIgnoreCase(type);

                if (wantEmail && person.getEmail() != null && !person.getEmail().isBlank()) {
                    EmailNotification email = new EmailNotification();
                    email.setRecipientEmailAddress(person.getEmail());
                    email.setPayload(payload);
                    emailNotificationSender.send(email);
                }

                if (wantSms) {
                    String phone = person.getCellPhone() != null && !person.getCellPhone().isBlank()
                            ? person.getCellPhone()
                            : person.getWorkPhone();
                    if (phone != null && !phone.isBlank()) {
                        SMSNotification sms = new SMSNotification();
                        sms.setReceiverPhoneNumber(phone);
                        sms.setPayload(payload);
                        smsNotificationSender.send(sms);
                    }
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "sendNotifications",
                        "Error sending notification to recipient " + recipient.getId() + ": " + e.getMessage());
            }
        }
    }

    private static class SimpleNotificationPayload implements NotificationPayload {
        private final String subject;
        private final String message;

        SimpleNotificationPayload(String subject, String message) {
            this.subject = subject;
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getSubject() {
            return subject;
        }
    }

    private static class EvaluationResult {
        private final boolean matched;
        private final List<String> matchedConditionIds;

        EvaluationResult(boolean matched, List<String> matchedConditionIds) {
            this.matched = matched;
            this.matchedConditionIds = matchedConditionIds;
        }

        boolean isMatched() {
            return matched;
        }

        List<String> getMatchedConditionIds() {
            return matchedConditionIds;
        }
    }
}
