<template>
  <div class="page">
    <h2 class="page-title">日终门禁 · 执行</h2>
    <p class="page-desc">正式轧差前先过检查单：逐项执行四类可开关检查，全部通过方可进入轧差</p>

    <div class="card-panel">
      <div class="toolbar" style="justify-content:space-between">
        <strong>检查单（可开关）</strong>
        <span v-if="!auth.isOperator" class="hint">只读账号仅可查看，开关与发起检查需操作员</span>
      </div>
      <el-table :data="checks" v-loading="loadingChecks">
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :disabled="!auth.isOperator || toggling"
              @change="(v) => onToggle(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="label" label="检查项" min-width="200" />
        <el-table-column prop="description" label="说明" min-width="320" />
      </el-table>
    </div>

    <div class="card-panel" style="margin-top:16px">
      <div class="toolbar">
        <el-date-picker v-model="businessDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日" />
        <el-button type="primary" :disabled="!auth.isOperator" :loading="running" @click="runGate">
          发起检查
        </el-button>
        <el-button @click="loadLatest">刷新结果</el-button>
      </div>

      <div v-if="latest" class="run-result">
        <div class="run-head">
          <strong>最近检查</strong>
          <el-tag :type="latest.status === 'PASSED' ? 'success' : 'danger'" style="margin-left:8px">
            {{ latest.status === 'PASSED' ? '门禁已通过' : '门禁未通过' }}
          </el-tag>
          <span class="hint" style="margin-left:12px">
            {{ latest.businessDate }} · 由 {{ latest.createdBy }} 于 {{ formatTime(latest.createdAt) }} 发起
          </span>
        </div>
        <div v-for="item in latest.results" :key="item.type" class="check-item">
          <div class="check-line">
            <el-tag v-if="!item.enabled" type="info" size="small">已关闭</el-tag>
            <el-tag v-else-if="item.passed" type="success" size="small">通过</el-tag>
            <el-tag v-else type="danger" size="small">未通过</el-tag>
            <span class="check-label">{{ item.label }}</span>
          </div>
          <ul v-if="item.enabled && !item.passed" class="fail-list">
            <li v-for="(d, i) in item.details" :key="i">{{ d }}</li>
          </ul>
        </div>
      </div>
      <el-empty v-else-if="!loadingLatest" description="该交割日尚未执行门禁检查，请发起检查" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const checks = ref([])
const businessDate = ref(new Date().toISOString().slice(0, 10))
const latest = ref(null)
const loadingChecks = ref(false)
const loadingLatest = ref(false)
const running = ref(false)
const toggling = ref(false)

function formatTime(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString()
}

async function loadChecks() {
  loadingChecks.value = true
  try {
    const { data } = await api.get('/eod-gate/checks')
    checks.value = data
  } finally {
    loadingChecks.value = false
  }
}

async function loadLatest() {
  loadingLatest.value = true
  try {
    const { data: status } = await api.get('/eod-gate/status', {
      params: { businessDate: businessDate.value }
    })
    if (status.gateRunId) {
      const { data } = await api.get(`/eod-gate/runs/${status.gateRunId}`)
      latest.value = data
    } else {
      latest.value = null
    }
  } finally {
    loadingLatest.value = false
  }
}

async function onToggle(row, value) {
  toggling.value = true
  try {
    await api.put(`/eod-gate/checks/${row.type}`, { enabled: value })
    ElMessage.success(`已${value ? '开启' : '关闭'}检查项：${row.label}`)
  } catch (e) {
    row.enabled = !value
  } finally {
    toggling.value = false
  }
}

async function runGate() {
  running.value = true
  try {
    const { data } = await api.post('/eod-gate/runs', { businessDate: businessDate.value })
    latest.value = data
    if (data.status === 'PASSED') {
      ElMessage.success('门禁检查全部通过，可以执行轧差')
    } else {
      ElMessage.warning(`门禁未通过：${data.failedCount} 项检查失败，请处理后再轧差`)
    }
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadChecks()
  loadLatest()
})
</script>

<style scoped>
.hint {
  color: var(--muted);
  font-size: 13px;
}
.run-result {
  border-top: 1px solid var(--line);
  padding-top: 12px;
}
.run-head {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.check-item {
  padding: 10px 0;
  border-top: 1px dashed var(--line);
}
.check-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.check-label {
  font-weight: 600;
}
.fail-list {
  margin: 8px 0 0;
  padding-left: 24px;
  color: #b42318;
  font-size: 13px;
}
.fail-list li {
  margin: 4px 0;
}
</style>
