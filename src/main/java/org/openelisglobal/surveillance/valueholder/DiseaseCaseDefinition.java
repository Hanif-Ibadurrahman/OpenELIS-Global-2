package org.openelisglobal.surveillance.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disease_case_definition")
public class DiseaseCaseDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "icd_code", nullable = false, length = 50)
    private String icdCode;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "effective_date")
    private Date effectiveDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "time_range", length = 20)
    private String timeRange;

    @Column(name = "threshold_type", length = 20)
    private String thresholdType;

    @Column(name = "threshold_value", precision = 18, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "next_notification")
    private Timestamp nextNotification;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "last_updated", nullable = false)
    private Timestamp lastUpdated;

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiseaseNotificationRecipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ruleGroup ASC, id ASC")
    private List<DiseaseCaseCondition> conditions = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<DiseaseCaseCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<DiseaseCaseCondition> conditions) {
        this.conditions = conditions;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Timestamp getNextNotification() {
        return nextNotification;
    }

    public void setNextNotification(Timestamp nextNotification) {
        this.nextNotification = nextNotification;
    }

    public List<DiseaseNotificationRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<DiseaseNotificationRecipient> recipients) {
        this.recipients = recipients;
    }
}
