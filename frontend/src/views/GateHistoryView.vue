<template>
  <div class="page">
    <h2 class="page-title">门禁历史</h2>
    <p class="page-desc">历次日终门禁运行结果，点开可查看逐项明细</p>

    <div class="toolbar">
      <el-button @click="load">刷新</el-button>
      <el-button type="primary" @click="$router.push('/gate')">返回门禁执行</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="runs" v-loading="loading" stripe>
        <el-table-column prop="runId" label="Run ID" min-width="220">
          <template #default="{ row }"><span class="mono">{{ row.runId }}</span></template>
        </el-table-column>
        <el-table-column prop="settleDate" label="交割日" width="120" />
        <el-table-column label="结论" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASSED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过 / 失败 / 跳过" width="160">
          <template #default="{ row }">
            <span class="pass">{{ row.passCount }}</span> /
            <span :class="{ fail: row.failCount > 0 }"> {{ row.failCount }}</span> /
            <span class="muted"> {{ row.skipCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="130">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="运行时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && runs.length === 0" class="empty-hint">
        暂无门禁运行记录。请到「门禁执行」页发起一次检查运行。
      </div>
    </div>

    <el-drawer v-model="drawer" size="52%" :title="detail ? `门禁运行 ${detail.runId}` : ''">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="交割日">{{ detail.settleDate }}</el-descriptions-item>
          <el-descriptions-item label="结论">
            <el-tag :type="detail.passed ? 'success' : 'danger'">{{ detail.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operator || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运行时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-collapse v-model="panels" style="margin-top:16px">
          <el-collapse-item v-for="r in detail.results" :key="r.checkType" :name="r.checkType">
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
            <el-timeline v-if="r.findings.length">
              <el-timeline-item v-for="(f, i) in r.findings" :key="i" type="danger">
                <div class="fid mono">{{ f.targetId || '-' }}</div>
                <div class="muted">
                  {{ targetTypeLabel(f.targetType) }}
                  <template v-if="f.currency"> · {{ f.currency }}</template>
                  <template v-if="f.amount"> · {{ f.amount }}</template>
                </div>
                <div>{{ f.detail }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-collapse-item>
        </el-collapse>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client'

const runs = ref([])
const loading = ref(false)
const drawer = ref(false)
const detail = ref(null)
const panels = ref([])

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

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/gate/runs')
    runs.value = data
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  const { data } = await api.get(`/gate/runs/${row.runId}`)
  detail.value = data
  panels.value = data.results.filter((r) => r.status === 'FAIL').map((r) => r.checkType)
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.pass {
  color: #16a34a;
  font-weight: 600;
}
.fail {
  color: #dc2626;
  font-weight: 600;
}
.muted {
  color: var(--muted);
  font-size: 12px;
}
.empty-hint {
  margin-top: 12px;
  padding: 28px;
  text-align: center;
  color: var(--muted);
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
  margin: 4px 0 10px;
}
.fid {
  font-size: 12px;
}
</style>
