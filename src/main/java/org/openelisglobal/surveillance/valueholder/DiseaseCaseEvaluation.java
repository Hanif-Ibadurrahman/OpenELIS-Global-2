package org.openelisglobal.surveillance.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "disease_case_evaluation")
public class DiseaseCaseEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "icd_code", nullable = false, length = 50)
    private String icdCode;

    @Column(name = "sample_id", length = 50)
    private String sampleId;

    @Column(name = "patient_id", length = 50)
    private String patientId;

    @Column(name = "region_id", length = 50)
    private String regionId;

    @Column(name = "evaluation_date", nullable = false)
    private Timestamp evaluationDate;

    @Column(name = "is_confirmed", nullable = false)
    private boolean isConfirmed = false;

    @Column(name = "matched_conditions", columnDefinition = "TEXT")
    private String matchedConditions;

    @Column(name = "definition_id")
    private Integer definitionId;

    @Column(name = "last_updated", nullable = false)
    private Timestamp lastUpdated;

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

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public Timestamp getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(Timestamp evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }

    public String getMatchedConditions() {
        return matchedConditions;
    }

    public void setMatchedConditions(String matchedConditions) {
        this.matchedConditions = matchedConditions;
    }

    public Integer getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Integer definitionId) {
        this.definitionId = definitionId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
