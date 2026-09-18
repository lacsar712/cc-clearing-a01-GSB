<template>
  <div class="page">
    <h2 class="page-title">日终门禁 · 运行历史</h2>
    <p class="page-desc">历次门禁检查运行记录，展开可查看逐项结果与失败原因</p>

    <div class="card-panel">
      <div class="toolbar" style="justify-content:space-between">
        <strong>检查运行记录</strong>
        <el-button @click="load">刷新</el-button>
      </div>
      <el-table :data="runs" v-loading="loading" stripe row-key="gateRunId">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="detail">
              <div v-for="item in row.results" :key="item.type" class="check-item">
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
          </template>
        </el-table-column>
        <el-table-column prop="gateRunId" label="Gate Run ID" min-width="220">
          <template #default="{ row }">
            <span class="mono">{{ row.gateRunId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessDate" label="交割日" width="120" />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASSED' ? 'success' : 'danger'">
              {{ row.status === 'PASSED' ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败项" width="90">
          <template #default="{ row }">{{ row.failedCount }}</template>
        </el-table-column>
        <el-table-column prop="createdBy" label="发起人" width="110" />
        <el-table-column label="发起时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && runs.length === 0" description="暂无门禁运行记录" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client'

const runs = ref([])
const loading = ref(false)

function formatTime(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString()
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/eod-gate/runs')
    runs.value = data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail {
  padding: 8px 24px;
}
.check-item {
  padding: 8px 0;
  border-top: 1px dashed var(--line);
}
.check-item:first-child {
  border-top: none;
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
  margin: 6px 0 0;
  padding-left: 24px;
  color: #b42318;
  font-size: 13px;
}
.fail-list li {
  margin: 4px 0;
}
</style>
