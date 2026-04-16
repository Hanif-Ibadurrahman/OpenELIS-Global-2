package org.openelisglobal.surveillance.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "disease_case_condition")
public class DiseaseCaseCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id", nullable = false)
    private DiseaseCaseDefinition definition;

    @Column(name = "rule_group", nullable = false)
    private Integer ruleGroup = 1;

    @Column(name = "operator", nullable = false, length = 5)
    private String operator = "AND";

    @Column(name = "test_name", nullable = false, length = 255)
    private String testName;

    @Column(name = "result_type", nullable = false, length = 5)
    private String resultType;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "numeric_operator", length = 5)
    private String numericOperator;

    @Column(name = "numeric_value", precision = 18, scale = 4)
    private BigDecimal numericValue;

    @Column(name = "last_updated", nullable = false)
    private Timestamp lastUpdated;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DiseaseCaseDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(DiseaseCaseDefinition definition) {
        this.definition = definition;
    }

    public Integer getRuleGroup() {
        return ruleGroup;
    }

    public void setRuleGroup(Integer ruleGroup) {
        this.ruleGroup = ruleGroup;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getNumericOperator() {
        return numericOperator;
    }

    public void setNumericOperator(String numericOperator) {
        this.numericOperator = numericOperator;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(BigDecimal numericValue) {
        this.numericValue = numericValue;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
