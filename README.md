# 分布式银行核心账务系统 - Core Ledger Platform

## 一、项目概述

本项目是基于分布式架构设计的银行核心账务系统，采用微服务架构设计，实现账户管理的核心功能。系统支持高并发、高可用，具备分布式事务处理能力。

## 二、技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.3.12.RELEASE | 基础框架 |
| Spring Cloud | Hoxton.SR12 | 微服务框架 |
| Spring Cloud Alibaba | 2.2.7.RELEASE | 阿里微服务套件 |
| Dubbo | 2.7.15 | RPC框架 |
| Seata | 1.5.2 | 分布式事务框架（TCC/Saga模式） |
| ShardingSphere | 5.1.1 | 分库分表中间件 |
| MyBatis-Plus | 3.5.1 | ORM框架 |
| MySQL | 8.0.x | 数据库 |
| Redis | 6.2.x | 缓存/分布式锁 |
| RocketMQ | 4.9.x | 消息队列 |
| Nacos | 2.1.x | 服务注册与配置中心 |
| Prometheus | 2.35.x | 监控指标收集 |
| Grafana | 8.5.x | 监控可视化 |
| Docker | 20.10+ | 容器化部署 |
| 麒麟V10 | - | 国产操作系统支持 |

## 三、项目结构

```
core-ledger-platform/
├── core-ledger-common/         # 公共模块
│   ├── src/main/java/com/bank/core/common/
│   │   ├── enums/              # 枚举类
│   │   ├── constants/          # 常量类
│   │   ├── result/             # 统一响应
│   │   ├── exception/          # 异常处理
│   │   └── utils/              # 工具类
│   └── pom.xml
├── core-ledger-api/            # API接口模块
│   ├── src/main/java/com/bank/core/api/
│   │   ├── dto/                # 数据传输对象
│   │   ├── vo/                 # 视图对象
│   │   ├── service/            # Dubbo服务接口
│   │   ├── tcc/                # TCC事务接口
│   │   └── event/              # 事件对象
│   └── pom.xml
├── core-ledger-account/        # 账户服务
│   ├── src/main/java/com/bank/core/account/
│   │   ├── AccountServiceApplication.java
│   │   ├── entity/             # 实体类
│   │   ├── mapper/             # Mapper接口
│   │   ├── service/            # 业务服务
│   │   │   └── impl/           # 服务实现
│   │   ├── controller/         # REST控制器
│   │   ├── consumer/           # 消息消费者
│   │   └── config/             # 配置类
│   ├── src/main/resources/
│   │   ├── bootstrap.yml
│   │   ├── application.yml
│   │   └── application-dev.yml
│   └── pom.xml
├── sql/                        # 数据库脚本
│   └── init.sql
├── docker/                     # Docker配置
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── seata/
│   ├── prometheus/
│   └── rocketmq/
├── nacos/                      # Nacos配置示例
└── pom.xml                     # 父POM
```

## 四、核心功能

### 4.1 账户管理

#### 4.1.1 账户创建
- **接口**：`POST /api/account/create`
- **参数**：
  - `userId`：用户ID（必填）
  - `accountType`：账户类型（1-个人，2-企业）（必填）
  - `currency`：币种（CNY/USD/EUR等）（必填）
  - `initBalance`：初始余额（必填，非负）
  - `requestId`：请求ID（用于幂等）
- **功能**：创建指定类型和币种的账户，自动生成账户号码

#### 4.1.2 账户信息查询
- **接口**：`GET /api/account/{accountId}`
- **参数**：`accountId` 账户ID
- **返回**：账户余额、状态、开户时间等信息

#### 4.1.3 根据账号查询
- **接口**：`GET /api/account/no/{accountNo}`
- **参数**：`accountNo` 账户号码
- **返回**：账户完整信息

#### 4.1.4 账户冻结
- **接口**：`POST /api/account/freeze`
- **参数**：
  - `accountId`：账户ID（必填）
  - `freezeType`：冻结类型（1-司法冻结，2-风控冻结）（必填）
  - `remark`：冻结备注
  - `operator`：操作人
- **功能**：冻结账户，记录冻结日志，账户状态变为"冻结"

#### 4.1.5 账户解冻
- **接口**：`POST /api/account/unfreeze`
- **参数**：
  - `accountId`：账户ID（必填）
  - `freezeType`：冻结类型（必填）
  - `remark`：解冻备注
  - `operator`：操作人
- **功能**：解冻账户，账户状态恢复为"正常"

#### 4.1.6 账户销户
- **接口**：`POST /api/account/close`
- **参数**：
  - `accountId`：账户ID（必填）
  - `remark`：销户备注
  - `operator`：操作人
- **功能**：余额清零，状态变为"已销户"，账户不可再使用

## 五、架构设计

### 5.1 分布式事务设计

采用Seata TCC模式实现分布式事务：

- **Try阶段**：执行业务检查，预留资源，记录事务日志
- **Confirm阶段**：确认执行业务操作，幂等处理
- **Cancel阶段**：回滚业务操作，恢复数据

### 5.2 分库分表设计

- **分库策略**：按`userId`哈希取模，分为2个库（ds0, ds1）
- **分表策略**：按`accountId`哈希取模，账户表分为4张，TCC日志表分为2张
- **主键生成**：采用雪花算法生成分布式ID

### 5.3 缓存设计

- **账户信息缓存**：Redis缓存账户查询结果，过期时间5分钟
- **分布式锁**：基于Redisson实现分布式锁，防止并发操作
- **幂等控制**：基于Redis实现请求幂等性校验

### 5.4 事件驱动设计

账户状态变更通过RocketMQ发送事件：
- Topic：`account-topic`
- Tags：`create`/`freeze`/`unfreeze`/`close`

## 六、部署指南

### 6.1 本地开发环境

#### 前置条件
- JDK 1.8+
- Maven 3.6+
- Docker 20.10+
- Docker Compose 1.29+

#### 启动步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd core-ledger-platform
```

2. **启动基础设施**
```bash
cd docker
docker-compose up -d mysql nacos redis rocketmq-namesrv rocketmq-broker seata-server prometheus grafana
```

3. **导入Nacos配置**
- 访问Nacos控制台：http://localhost:8848/nacos
- 用户名：nacos，密码：nacos
- 创建命名空间 `public`（已默认存在）
- 导入配置文件到 `CORE_LEDGER_GROUP` 分组：
  - `common-database.yaml`
  - `common-redis.yaml`
  - `common-rocketmq.yaml`
  - `core-ledger-account.yaml`

4. **编译项目**
```bash
cd ..
mvn clean install -DskipTests
```

5. **启动账户服务**
```bash
cd core-ledger-account
mvn spring-boot:run
```

### 6.2 Docker部署

1. **编译项目**
```bash
mvn clean package -DskipTests
```

2. **启动所有服务**
```bash
cd docker
docker-compose up -d
```

### 6.3 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 账户服务 | http://localhost:8081/account | 账户服务API |
| Nacos控制台 | http://localhost:8848/nacos | 服务注册与配置中心 |
| Seata控制台 | http://localhost:7091 | 分布式事务控制台 |
| Prometheus | http://localhost:9090 | 监控指标 |
| Grafana | http://localhost:3000 | 监控可视化（admin/admin123） |
| Swagger | http://localhost:8081/account/swagger-ui.html | API文档 |

## 七、API示例

### 7.1 创建账户

**请求**：
```bash
curl -X POST http://localhost:8081/account/api/account/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "U001",
    "accountType": 1,
    "currency": "CNY",
    "initBalance": 1000.00,
    "requestId": "REQ001"
  }'
```

**响应**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "accountId": "1689000000000",
    "accountNo": "6222021234567890",
    "userId": "U001",
    "accountType": 1,
    "accountTypeDesc": "个人账户",
    "currency": "CNY",
    "currencyDesc": "人民币",
    "balance": 1000.00,
    "status": 1,
    "statusDesc": "正常",
    "createTime": "2024-01-01 10:00:00"
  },
  "timestamp": 1704067200000
}
```

### 7.2 查询账户

**请求**：
```bash
curl http://localhost:8081/account/api/account/1689000000000
```

### 7.3 冻结账户

**请求**：
```bash
curl -X POST http://localhost:8081/account/api/account/freeze \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "1689000000000",
    "freezeType": 2,
    "remark": "风控检测异常交易",
    "operator": "admin"
  }'
```

## 八、监控指标

系统暴露以下Prometheus指标：

| 指标名称 | 说明 |
|----------|------|
| `account_create_total` | 账户创建总数 |
| `account_freeze_total` | 账户冻结总数 |
| `account_unfreeze_total` | 账户解冻总数 |
| `account_close_total` | 账户销户总数 |
| `account_active_count` | 活跃账户数 |
| `account_create_http_duration_seconds` | HTTP创建账户耗时 |
| `account_get_http_duration_seconds` | HTTP查询账户耗时 |
| `account_create_duration_seconds` | Dubbo创建账户耗时 |

## 九、数据库表结构

### t_account（账户表，分4张）
- 主键：id（雪花ID）
- 唯一键：account_id, account_no
- 索引：user_id, (user_id, account_type, currency)

### t_account_freeze_log（冻结日志表，分4张）
- 记录每次冻结/解冻操作
- 用于审计追踪

### t_account_tcc_log（TCC事务日志表，分2张）
- 记录TCC事务各阶段执行情况
- 用于事务恢复和幂等控制

### undo_log（SEATA UNDO日志表）
- SEATA AT模式所需
- 用于事务回滚

## 十、注意事项

1. **幂等性**：所有写操作都支持requestId幂等控制，建议每次请求生成唯一requestId
2. **分布式锁**：账户操作会获取分布式锁，防止并发操作问题
3. **TCC空回滚**：TCC实现支持空回滚和防悬挂
4. **分库分表**：查询条件必须包含分片键，否则会全表扫描
5. **事务边界**：Seata全局事务注解需要正确配置，避免事务失效
6. **消息消费**：RocketMQ消费者需要处理重复消费问题

## 十一、扩展规划

1. 账户流水模块
2. 转账交易模块（支持TCC/Saga）
3. 利息计算模块
4. 报表统计模块
5. 风控规则引擎
6. 对账清算模块

## 十二、许可证

Copyright © 2024 Core Ledger Platform. All Rights Reserved.
