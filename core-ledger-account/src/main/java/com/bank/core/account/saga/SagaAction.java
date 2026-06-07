package com.bank.core.account.saga;

import java.util.Map;

public interface SagaAction {

    boolean forward(String sagaId, Map<String, Object> params);

    boolean compensate(String sagaId, Map<String, Object> params);

    String getServiceName();
}
