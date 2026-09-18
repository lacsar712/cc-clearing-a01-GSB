# 多币种轧差清算工作台（Clearing Netting Workbench）

单币种多边轧差清算全栈演示：录入义务 → 日终门禁检查 → 执行轧差 → 查看净头寸 → 确认 settle。

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
| operator | op123456 | 可写（门禁开关/发起检查、轧差、settle、新建会员/义务） |
| viewer | view123456 | 只读 |

## 日终门禁（EOD Gate）

正式轧差前先过检查单。固定四类可开关检查（非通用规则引擎）：

| 检查项 | 规则 |
|--------|------|
| 停用会员仍挂 OPEN 义务 | 交割日当日 OPEN 义务的付款/收款方不得为已停用会员 |
| 同日多币种混用 | 同一交割日的 OPEN 义务只允许单一币种 |
| 金额非正 | 交割日当日 OPEN 义务金额必须为正数 |
| 当日 FAILED 批次未处理 | 交割日当日不得残留 FAILED 状态的轧差批次 |

- 「日终门禁 → 门禁执行」：开关检查项、选择交割日发起检查，逐项展示通过/失败原因
- 「日终门禁 → 运行历史」：历次检查运行记录，展开看逐项明细
- 「轧差执行」页会同步提示所选交割日的门禁状态（已通过 / 未通过 / 未检查）
- 开关与发起检查限操作员；只读账号只能查看结果

主要接口：

```
GET  /api/eod-gate/checks                 检查项与开关（登录即可）
PUT  /api/eod-gate/checks/{type}          开关检查项（操作员）body: {"enabled": true}
POST /api/eod-gate/runs                   发起检查运行（操作员）body: {"businessDate": "2026-09-17"}
GET  /api/eod-gate/runs                   运行历史
GET  /api/eod-gate/runs/{id}              单次运行逐项结果
GET  /api/eod-gate/status?businessDate=   某交割日最近一次门禁状态
```

## Verification

1. 打开 http://localhost:3171 ，使用 `operator` / `op123456` 登录
2. 首页查看 seed 灌入的待轧差义务摘要与最近批次
3. 「会员」页确认演示会员状态（Delta Trading 为 SUSPENDED，且仍挂当日 OPEN 义务）
4. 「日终门禁 → 门禁执行」选择当天交割日，点击「发起检查」：
   - 「停用会员仍挂 OPEN 义务」未通过，并列出 Delta Trading 的义务明细
5. 到「会员」页将 Delta Trading 启用（ACTIVE），回到门禁执行页再次「发起检查」，全部通过
6. 「日终门禁 → 运行历史」确认两次运行一 FAILED 一 PASSED，展开可见逐项结果
7. 「轧差执行」页顶部同步显示门禁状态；选择 settleDate + currency（如 USD）执行轧差
8. 确认净头寸表 ΣnetAmount = 0，批次状态 COMPLETED
9. 进入批次详情，点击 Settle，义务变为 SETTLED
10. 使用 `viewer` 登录，确认只能浏览、无法执行写操作（门禁开关、发起检查、轧差均被拒）

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

发起一次门禁检查（操作员）：

```bash
curl -X POST http://localhost:8171/api/eod-gate/runs \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d "{\"businessDate\":\"$(date +%F)\"}"
```

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
