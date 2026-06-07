package com.bank.core.account.saga;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SagaStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stepId;

    private String stepName;

    private Integer stepOrder;

    private String serviceName;

    private String forwardMethod;

    private String compensateMethod;

    private Map<String, Object> params;

    private Integer status;

    private Integer retryCount = 0;

    private Integer maxRetryCount = 3;

    private String errorMessage;

    private java.time.LocalDateTime executeTime;

    private java.time.LocalDateTime completeTime;

    public SagaStep() {
    }

    public SagaStep(String stepId, String stepName, Integer stepOrder,
                    String serviceName, String forwardMethod, String compensateMethod) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.serviceName = serviceName;
        this.forwardMethod = forwardMethod;
        this.compensateMethod = compensateMethod;
    }
}
