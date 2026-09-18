<template>
  <div class="page">
    <h2 class="page-title">轧差执行</h2>
    <p class="page-desc">指定交割日与币种执行单币种多边轧差，校验 Σnet = 0</p>

    <div class="card-panel">
      <div class="toolbar">
        <el-date-picker v-model="settleDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日" />
        <el-select v-model="currency" style="width:120px">
          <el-option label="USD" value="USD" />
          <el-option label="CNY" value="CNY" />
          <el-option label="EUR" value="EUR" />
        </el-select>
        <el-button
          type="primary"
          :disabled="!auth.isOperator || !gate.passed"
          :loading="running"
          @click="execute"
        >执行轧差</el-button>
        <el-button @click="loadRuns">刷新批次</el-button>
      </div>

      <el-alert
        :closable="false"
        show-icon
        :type="gate.passed ? 'success' : gate.hasRun ? 'error' : 'warning'"
        class="gate-alert"
      >
        <template #title>
          <template v-if="gate.passed">
            门禁已通过（交割日 {{ settleDate }}），可以执行轧差。
            <router-link to="/gate" class="gate-link">查看门禁</router-link>
          </template>
          <template v-else-if="gate.hasRun">
            日终门禁未通过，轧差已被拦截。请先到
            <router-link to="/gate" class="gate-link">日终门禁</router-link>
            修复全部失败项后重跑。
          </template>
          <template v-else>
            交割日 {{ settleDate }} 尚未运行日终门禁，正式轧差前必须先通过检查。
            <router-link to="/gate" class="gate-link">前往门禁 →</router-link>
          </template>
        </template>
      </el-alert>
    </div>

    <div v-if="result" class="card-panel" style="margin-top:16px">
      <div class="toolbar" style="justify-content:space-between">
        <div>
          <strong>本次结果</strong>
          <el-tag style="margin-left:8px" :type="result.run.status === 'COMPLETED' ? 'success' : 'danger'">
            {{ result.run.status }}
          </el-tag>
          <span style="margin-left:12px">ΣnetAmount = {{ result.sumNetAmount }}</span>
        </div>
        <el-button link type="primary" @click="$router.push(`/netting-runs/${result.run.runId}`)">查看详情</el-button>
      </div>
      <el-table :data="result.positions" stripe>
        <el-table-column prop="memberId" label="会员 ID" min-width="220">
          <template #default="{ row }">
            <span class="mono">{{ row.memberId }}</span>
            <div>{{ nameOf(row.memberId) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="90" />
        <el-table-column prop="netAmount" label="净头寸（正应收/负应付）" min-width="200" />
      </el-table>
    </div>

    <div class="card-panel" style="margin-top:16px">
      <strong>历史批次</strong>
      <el-table :data="runs" v-loading="loading" stripe style="margin-top:12px">
        <el-table-column prop="runId" label="Run ID" min-width="220">
          <template #default="{ row }">
            <router-link class="mono" :to="`/netting-runs/${row.runId}`">{{ row.runId }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="settleDate" label="交割日" width="120" />
        <el-table-column prop="currency" label="币种" width="90" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="failureReason" label="失败原因" min-width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const settleDate = ref(new Date().toISOString().slice(0, 10))
const currency = ref('USD')
const running = ref(false)
const loading = ref(false)
const result = ref(null)
const runs = ref([])
const memberMap = ref({})
const gate = reactive({ passed: false, hasRun: false, runId: null, status: 'NOT_RUN' })

function nameOf(id) {
  return memberMap.value[id] || ''
}

async function loadGate() {
  if (!settleDate.value) {
    gate.passed = false
    gate.hasRun = false
    gate.runId = null
    gate.status = 'NOT_RUN'
    return
  }
  try {
    const { data } = await api.get('/gate/status', { params: { settleDate: settleDate.value } })
    gate.passed = data.passed
    gate.hasRun = data.hasRun
    gate.runId = data.runId
    gate.status = data.status
  } catch {
    gate.passed = false
    gate.hasRun = false
  }
}

async function loadRuns() {
  loading.value = true
  try {
    const [r, m] = await Promise.all([api.get('/netting-runs'), api.get('/members')])
    runs.value = r.data
    memberMap.value = Object.fromEntries(m.data.map((x) => [x.memberId, x.name]))
  } finally {
    loading.value = false
  }
}

async function execute() {
  running.value = true
  try {
    const { data } = await api.post('/netting-runs', {
      settleDate: settleDate.value,
      currency: currency.value
    })
    result.value = data
    ElMessage.success('轧差完成，守恒校验通过')
    await loadRuns()
    await loadGate()
  } catch (e) {
    result.value = null
    await loadRuns()
    await loadGate()
  } finally {
    running.value = false
  }
}

watch(settleDate, loadGate)

onMounted(async () => {
  await Promise.all([loadRuns(), loadGate()])
})
</script>

<style scoped>
.gate-alert {
  margin-top: 4px;
}
.gate-link {
  color: var(--el-color-primary);
  font-weight: 600;
  margin-left: 4px;
}
</style>
