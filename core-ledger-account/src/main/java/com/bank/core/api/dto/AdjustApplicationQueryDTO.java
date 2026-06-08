package com.bank.core.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdjustApplicationQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;

    private Integer status;

    private Integer adjustType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String applicant;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
