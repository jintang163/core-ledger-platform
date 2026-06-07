package com.bank.core.account.saga;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SagaTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sagaId;

    private String sagaName;

    private String businessNo;

    private Integer status;

    private List<SagaStep> steps = new ArrayList<>();

    private Integer currentStepIndex = 0;

    private Boolean compensating = false;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime completeTime;

    private String errorMessage;

    public void addStep(SagaStep step) {
        this.steps.add(step);
    }

    public SagaStep getCurrentStep() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }

    public boolean moveToNextStep() {
        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            return true;
        }
        return false;
    }

    public boolean moveToPrevStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--;
            return true;
        }
        return false;
    }

    public boolean isCompleted() {
        return currentStepIndex >= steps.size();
    }

    public boolean isAllCompensated() {
        return compensating && currentStepIndex < 0;
    }
}
