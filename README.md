# 多币种轧差清算工作台（Clearing Netting Workbench）

单币种多边轧差清算全栈演示：录入义务 → **日终门禁逐项检查** → 执行轧差 → 查看净头寸 → 确认 settle。

> 日终门禁（Gate）：正式轧差前必须先对当日交割日跑完检查单；存在失败项时轧差被后端拦截。

## How to Run

```bash
cd projects/01-clearing-netting
docker compose up --build
```

镜像默认走 `docker.m.daocloud.io` 与 Maven/npm 国内源，便于在受限网络下构建。若本机已有同名官方镜像亦可直接使用。

后台运行：

```bash
docker compose up --build -d
```

停止：

```bash
docker compose down
```

## Services

| 服务 | 宿主机地址 |
|------|------------|
| Frontend | http://localhost:3171 |
| Backend API | http://localhost:8171 |
| PostgreSQL | localhost:54371 |

容器内：backend 监听 `8080`，frontend nginx 将 `/api` 反代到 `backend:8080`。

## 测试账号

| 用户名 | 密码 | 权限 |
|--------|------|------|
| operator | op123456 | 可写（轧差、settle、新建会员/义务） |
| viewer | view123456 | 只读 |

## Verification

1. 打开 http://localhost:3171 ，使用 `operator` / `op123456` 登录
2. 首页查看 seed 灌入的待轧差义务摘要与最近批次
3. 左侧导航进入「日终门禁 → 门禁执行」，选择当日交割日，点击「发起检查运行」
4. 首次运行会出现 **4 个失败项**（系统启动时已灌入演示脏数据），逐项展开可看到具体对象与原因：
   - 停用会员挂 OPEN 义务
   - 同日多币种混用
   - 金额非正
   - 当日 FAILED 批次未处理
5. 在每个失败项的「修复」列，将问题义务「取消」、将 FAILED 批次「确认已处理」
6. 数据修好后再次「发起检查运行」，四项全部 **PASSED**
7. 进入「轧差执行」，顶部提示「门禁已通过」，选择对应交割日 + 币种执行轧差（门禁未通过时按钮禁用，后端同样拦截，返回 `GATE_NOT_PASSED`）
8. 确认净头寸表 ΣnetAmount = 0，批次状态 COMPLETED；进入批次详情 Settle
9. 「日终门禁 → 门禁历史」打开即可看到历次运行（含一条前一交割日的演示 PASSED 记录）及逐项明细
10. 使用 `viewer` / `view123456` 登录，确认可浏览门禁结果与历史，但开关、发起检查、取消、确认等写操作均不可用

> 检查项开关位于门禁执行页顶部，仅操作员可调整；停用的检查项运行时标记为 SKIPPED。
> 权限模型保持为单一 `OPERATOR / VIEWER` 二元角色（非通用权限产品），端口仍为 3171 / 8171 / 54371。

### 原有轧差流程

1. 「会员」页确认演示会员为 ACTIVE；可新建或启停
2. 「义务」页筛选 OPEN 义务，或新建一笔同币种义务
3. 「轧差执行」选择 settleDate + currency（如 USD），执行轧差（需先通过门禁）
4. 确认净头寸表 ΣnetAmount = 0，批次状态 COMPLETED
5. 进入批次详情，点击 Settle，义务变为 SETTLED

健康检查：

```bash
curl http://localhost:8171/api/health
```

登录：

```bash
curl -X POST http://localhost:8171/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"operator\",\"password\":\"op123456\"}"
```

日终门禁 API（除 GET 外仅 OPERATOR）：

| 方法 & 路径 | 说明 |
|-------------|------|
| `GET  /api/gate/configs` | 四项检查及开关 |
| `POST /api/gate/configs/{checkType}` | 启用/停用检查项 `{"enabled":true}` |
| `POST /api/gate/runs` | 发起一次检查运行 `{"settleDate":"2026-09-17"}` |
| `GET  /api/gate/runs` | 历史运行列表 |
| `GET  /api/gate/runs/{id}` | 某次运行逐项结果与问题明细 |
| `GET  /api/gate/status?settleDate=` | 该交割日最近一次门禁是否通过（轧差页用） |
| `POST /api/obligations/{id}/cancel` | 取消 OPEN 义务（修复手段） |
| `POST /api/netting-runs/{id}/acknowledge` | 确认已处理 FAILED 批次 |

## 测试

```bash
cd backend
mvn test
```

- `GateCheckEvaluatorTest`：四类检查的通过/失败/停用/主币种判定（纯领域）
- `GateEndToEndTest`：H2 + 真实 HTTP，跑通「门禁四失败 → 轧差被拦 → 取消/确认 → 重跑通过 → 轧差成功」，并校验 viewer 写操作 403

## 技术栈

- Backend: Java 17、Spring Boot 3、Hexagonal、JPA、PostgreSQL、JWT
- Frontend: Vue 3、Vite、Element Plus、Pinia、Vue Router、nginx
- Infra: Docker Compose（db / backend / seed / frontend）

## 项目结构

```
01-clearing-netting/
├── PRD.md
├── README.md
├── docker-compose.yml
├── backend/
├── frontend/
└── seed/
```
