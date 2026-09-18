<template>
  <div class="page">
    <h2 class="page-title">日终门禁</h2>
    <p class="page-desc">正式轧差前按检查单逐项核验；存在失败项时不得执行轧差</p>

    <div class="card-panel">
      <div class="toolbar" style="justify-content:space-between; margin-bottom:8px">
        <strong>检查项开关</strong>
        <el-tag size="small" :type="auth.isOperator ? 'success' : 'info'">
          {{ auth.isOperator ? '操作员：可调整开关并发起检查' : '只读账号：仅查看结果' }}
        </el-tag>
      </div>
      <div class="switch-row" v-for="c in configs" :key="c.checkType">
        <div class="switch-meta">
          <el-switch
            :model-value="c.enabled"
            :disabled="!auth.isOperator || toggling === c.checkType"
            @change="(v) => toggle(c, v)"
          />
          <div>
            <div class="switch-title">{{ c.title }}</div>
            <div class="switch-desc">{{ c.description }}</div>
          </div>
        </div>
        <el-tag size="small" :type="c.enabled ? 'success' : 'info'">{{ c.enabled ? '启用' : '已停用' }}</el-tag>
      </div>
    </div>

    <div class="card-panel" style="margin-top:16px">
      <div class="toolbar" style="justify-content:space-between">
        <div class="toolbar" style="margin-bottom:0">
          <el-date-picker
            v-model="settleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="交割日"
            :disabled="!auth.isOperator"
          />
          <el-button
            type="primary"
            :disabled="!auth.isOperator"
            :loading="running"
            @click="runGate"
          >发起检查运行</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
        <el-button link type="primary" @click="$router.push('/gate/history')">查看历史 →</el-button>
      </div>

      <div v-if="!latest" class="empty-hint">
        该交割日暂无门禁运行记录。{{ auth.isOperator ? '选择交割日后点击「发起检查运行」。' : '请等待操作员发起检查。' }}
      </div>

      <template v-else>
        <el-alert
          class="run-banner"
          :closable="false"
          :type="latest.passed ? 'success' : 'error'"
          show-icon
        >
          <template #title>
            最近一次运行（{{ formatTime(latest.createdAt) }}，{{ latest.operator || '-' }}）：
            <strong>{{ latest.passed ? 'PASSED · 全部通过，可执行轧差' : 'FAILED · 存在失败项，轧差已被拦截' }}</strong>
          </template>
        </el-alert>

        <el-collapse v-model="activePanels">
          <el-collapse-item
            v-for="r in latest.results"
            :key="r.checkType"
            :name="r.checkType"
          >
            <template #title>
              <div class="item-title">
                <el-tag size="small" :type="statusType(r.status)" effect="dark" class="status-tag">
                  {{ statusLabel(r.status) }}
                </el-tag>
                <span class="item-name">{{ r.title }}</span>
                <el-tag size="small" v-if="r.findings.length" type="danger" effect="plain">
                  {{ r.findings.length }} 项
                </el-tag>
              </div>
            </template>

            <div class="item-msg">{{ r.message }}</div>

            <el-table v-if="r.findings.length" :data="r.findings" size="small" stripe class="finding-table">
              <el-table-column label="对象" min-width="220">
                <template #default="{ row }">
                  <span class="mono">{{ row.targetId || '-' }}</span>
                  <div class="muted">{{ targetTypeLabel(row.targetType) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="会员 / 币种 / 金额" min-width="220">
                <template #default="{ row }">
                  <div>{{ row.memberName || row.memberId || '-' }}</div>
                  <div class="muted">
                    {{ row.currency || '-' }}
                    <span v-if="row.amount"> · {{ row.amount }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="detail" label="原因" min-width="260" />
              <el-table-column v-if="auth.isOperator" label="修复" width="240">
                <template #default="{ row }">
                  <el-button
                    v-if="row.targetType === 'OBLIGATION'"
                    size="small"
                    type="warning"
                    :loading="cancelling === row.targetId"
                    @click="cancelObligation(row.targetId)"
                  >取消该义务</el-button>
                  <el-button
                    v-else-if="row.targetType === 'NETTING_RUN'"
                    size="small"
                    type="success"
                    :loading="acknowledging === row.targetId"
                    @click="acknowledge(row.targetId)"
                  >确认已处理</el-button>
                  <span v-else class="muted">请到对应页面处理</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()

const today = new Date().toISOString().slice(0, 10)
const settleDate = ref(route.query.date || today)
const configs = ref([])
const latest = ref(null)
const activePanels = ref([])
const running = ref(false)
const toggling = ref(null)
const cancelling = ref(null)
const acknowledging = ref(null)

function statusType(s) {
  if (s === 'PASS') return 'success'
  if (s === 'FAIL') return 'danger'
  return 'info'
}
function statusLabel(s) {
  return s === 'PASS' ? '通过' : s === 'FAIL' ? '失败' : '跳过'
}
function targetTypeLabel(t) {
  return t === 'OBLIGATION' ? '义务' : t === 'NETTING_RUN' ? '轧差批次' : t === 'MEMBER' ? '会员' : t
}
function formatTime(v) {
  return v ? new Date(v).toLocaleString() : '-'
}

async function loadConfigs() {
  const { data } = await api.get('/gate/configs')
  configs.value = data
}

async function loadLatest() {
  if (!settleDate.value) {
    latest.value = null
    return
  }
  const { data } = await api.get('/gate/status', { params: { settleDate: settleDate.value } })
  if (data.hasRun) {
    const detail = await api.get(`/gate/runs/${data.runId}`)
    latest.value = detail.data
    activePanels.value = detail.data.results.filter((r) => r.status === 'FAIL').map((r) => r.checkType)
  } else {
    latest.value = null
    activePanels.value = []
  }
}

async function load() {
  await Promise.all([loadConfigs(), loadLatest()])
}

async function toggle(c, enabled) {
  toggling.value = c.checkType
  try {
    await api.post(`/gate/configs/${c.checkType}`, { enabled })
    ElMessage.success(`检查项已${enabled ? '启用' : '停用'}`)
    await loadConfigs()
  } finally {
    toggling.value = null
  }
}

async function runGate() {
  running.value = true
  try {
    const { data } = await api.post('/gate/runs', { settleDate: settleDate.value })
    latest.value = data
    activePanels.value = data.results.filter((r) => r.status === 'FAIL').map((r) => r.checkType)
    if (data.passed) {
      ElMessage.success('门禁全部通过，可以执行轧差')
    } else {
      const fails = data.results.filter((r) => r.status === 'FAIL').length
      ElMessage.warning(`门禁未通过：${fails} 个检查项失败，请逐项修复`)
    }
    await loadConfigs()
  } finally {
    running.value = false
  }
}

async function cancelObligation(id) {
  try {
    await ElMessageBox.confirm('确定取消该笔 OPEN 义务？取消后状态变为 CANCELLED。', '修复确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  cancelling.value = id
  try {
    await api.post(`/obligations/${id}/cancel`)
    ElMessage.success('义务已取消')
    await loadLatest()
  } finally {
    cancelling.value = null
  }
}

async function acknowledge(id) {
  acknowledging.value = id
  try {
    await api.post(`/netting-runs/${id}/acknowledge`)
    ElMessage.success('已确认处理该 FAILED 批次')
    await loadLatest()
  } finally {
    acknowledging.value = null
  }
}

watch(settleDate, loadLatest)

onMounted(load)
</script>

<style scoped>
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 4px;
  border-top: 1px solid var(--line);
}
.switch-meta {
  display: flex;
  align-items: center;
  gap: 14px;
}
.switch-title {
  font-weight: 600;
}
.switch-desc {
  color: var(--muted);
  font-size: 13px;
}
.empty-hint {
  margin-top: 12px;
  padding: 28px;
  text-align: center;
  color: var(--muted);
  border: 1px dashed var(--line);
  border-radius: 8px;
}
.run-banner {
  margin: 12px 0;
}
.item-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.status-tag {
  width: 52px;
  justify-content: center;
}
.item-name {
  font-weight: 600;
}
.item-msg {
  color: var(--text);
  margin: 4px 0 10px;
}
.finding-table {
  width: 100%;
}
.muted {
  color: var(--muted);
  font-size: 12px;
}
</style>
