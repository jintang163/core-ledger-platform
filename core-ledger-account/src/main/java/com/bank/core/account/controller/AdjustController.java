package com.bank.core.account.controller;

import com.bank.core.api.dto.*;
import com.bank.core.api.vo.AdjustApplicationVO;
import com.bank.core.common.result.Result;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = "人工调账管理")
@RestController
@RequestMapping("/api/adjust")
@RequiredArgsConstructor
@Validated
public class AdjustController {

    @PostMapping("/create")
    @ApiOperation("创建调账申请")
    public Result<AdjustApplicationVO> createApplication(@RequestBody @Valid AdjustApplicationCreateDTO dto) {
        log.info("创建调账申请, accountId: {}, adjustType: {}, amount: {}",
                dto.getAccountId(), dto.getAdjustType(), dto.getAmount());

        AdjustApplicationVO vo = new AdjustApplicationVO();
        vo.setId(SnowflakeIdGenerator.nextIdStr());
        vo.setApplicationNo("ADJ" + System.currentTimeMillis());
        vo.setAccountId(dto.getAccountId());
        vo.setAccountNo("ACC" + dto.getAccountId());
        vo.setUserId("USER" + dto.getAccountId());
        vo.setAdjustType(dto.getAdjustType());
        vo.setAdjustTypeDesc(dto.getAdjustType() == 1 ? "增加余额" : "扣减余额");
        vo.setAmount(dto.getAmount());
        vo.setCurrency("CNY");
        vo.setReason(dto.getReason());
        vo.setStatus(0);
        vo.setStatusDesc("待审批");
        vo.setApplicant("运营管理员");
        vo.setApplyTime(LocalDateTime.now());
        vo.setRemark(dto.getRemark());

        return Result.success(vo);
    }

    @PostMapping("/approve")
    @ApiOperation("审批调账申请")
    public Result<AdjustApplicationVO> approveApplication(@RequestBody @Valid AdjustApplicationApproveDTO dto) {
        log.info("审批调账申请, id: {}, approved: {}", dto.getId(), dto.getApproved());

        AdjustApplicationVO vo = new AdjustApplicationVO();
        vo.setId(dto.getId());
        vo.setStatus(dto.getApproved() ? 1 : 2);
        vo.setStatusDesc(dto.getApproved() ? "已审批" : "已拒绝");
        vo.setApprover("审批管理员");
        vo.setApproveTime(LocalDateTime.now());
        vo.setApproveRemark(dto.getApproveRemark());

        return Result.success(vo);
    }

    @PostMapping("/execute")
    @ApiOperation("执行调账")
    public Result<AdjustApplicationVO> executeApplication(@RequestBody @Valid AdjustApplicationExecuteDTO dto) {
        log.info("执行调账, id: {}", dto.getId());

        AdjustApplicationVO vo = new AdjustApplicationVO();
        vo.setId(dto.getId());
        vo.setStatus(3);
        vo.setStatusDesc("已执行");
        vo.setExecutor("运营管理员");
        vo.setExecuteTime(LocalDateTime.now());
        vo.setTransactionId("TXN" + SnowflakeIdGenerator.nextIdStr());

        return Result.success(vo);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询调账申请详情")
    public Result<AdjustApplicationVO> getApplication(@PathVariable String id) {
        log.info("查询调账申请详情, id: {}", id);

        AdjustApplicationVO vo = new AdjustApplicationVO();
        vo.setId(id);
        vo.setApplicationNo("ADJ202401010001");
        vo.setAccountId("ACC001");
        vo.setAccountNo("6222020000000001");
        vo.setUserId("USER001");
        vo.setAdjustType(1);
        vo.setAdjustTypeDesc("增加余额");
        vo.setAmount(new BigDecimal("1000.00"));
        vo.setCurrency("CNY");
        vo.setReason("客户投诉调账");
        vo.setStatus(0);
        vo.setStatusDesc("待审批");
        vo.setApplicant("运营管理员");
        vo.setApplyTime(LocalDateTime.now());
        vo.setRemark("测试调账");

        return Result.success(vo);
    }

    @PostMapping("/query")
    @ApiOperation("查询调账申请列表")
    public Result<Object> queryApplications(@RequestBody AdjustApplicationQueryDTO dto) {
        log.info("查询调账申请列表, status: {}, adjustType: {}", dto.getStatus(), dto.getAdjustType());

        List<AdjustApplicationVO> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AdjustApplicationVO vo = new AdjustApplicationVO();
            vo.setId("ADJ" + (i + 1));
            vo.setApplicationNo("ADJ20240101000" + (i + 1));
            vo.setAccountId("ACC00" + (i + 1));
            vo.setAccountNo("622202000000000" + (i + 1));
            vo.setUserId("USER00" + (i + 1));
            vo.setAdjustType(i % 2 == 0 ? 1 : 2);
            vo.setAdjustTypeDesc(i % 2 == 0 ? "增加余额" : "扣减余额");
            vo.setAmount(new BigDecimal((i + 1) * 100));
            vo.setCurrency("CNY");
            vo.setReason("调账原因" + (i + 1));
            vo.setStatus(i % 4);
            String[] statusDescs = {"待审批", "已审批", "已拒绝", "已执行"};
            vo.setStatusDesc(statusDescs[i % 4]);
            vo.setApplicant("运营管理员");
            vo.setApplyTime(LocalDateTime.now().minusHours(i));
            list.add(vo);
        }

        return Result.success(list);
    }
}
