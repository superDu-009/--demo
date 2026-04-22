<!-- views/tabs/ApiCost.vue — API消耗统计页面 -->
<template>
  <div class="api-cost-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card card-glass">
          <div class="stat-header">
            <h4 class="stat-title">总调用次数</h4>
            <el-icon :size="28" class="stat-icon"><DataLine /></el-icon>
          </div>
          <p class="stat-value text-neon">2,856</p>
          <p class="stat-trend up">较上周 ↑ 12.5%</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card card-glass">
          <div class="stat-header">
            <h4 class="stat-title">总消耗金额</h4>
            <el-icon :size="28" class="stat-icon"><Money /></el-icon>
          </div>
          <p class="stat-value text-neon">¥ 428.50</p>
          <p class="stat-trend up">较上周 ↑ 8.2%</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card card-glass">
          <div class="stat-header">
            <h4 class="stat-title">平均单次成本</h4>
            <el-icon :size="28" class="stat-icon"><Coin /></el-icon>
          </div>
          <p class="stat-value text-neon">¥ 0.15</p>
          <p class="stat-trend down">较上周 ↓ 3.1%</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card card-glass">
          <div class="stat-header">
            <h4 class="stat-title">失败率</h4>
            <el-icon :size="28" class="stat-icon"><Warning /></el-icon>
          </div>
          <p class="stat-value text-neon">2.3%</p>
          <p class="stat-trend down">较上周 ↓ 0.8%</p>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <div class="chart-card card-glass">
      <h3 class="chart-title">近7天消耗趋势</h3>
      <div class="chart-placeholder">
        <p>图表功能开发中</p>
      </div>
    </div>

    <!-- 消耗明细表格 -->
    <div class="table-card card-glass">
      <h3 class="table-title">消耗明细</h3>
      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="type" label="类型">
          <template #default="scope">
            <el-tag :type="scope.row.type === '图片生成' ? 'primary' : 'warning'">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="调用次数" />
        <el-table-column prop="cost" label="消耗金额" />
        <el-table-column prop="successRate" label="成功率" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataLine, Money, Coin, Warning } from '@element-plus/icons-vue'

const tableData = [
  { date: '2026-04-22', type: '图片生成', count: 256, cost: '¥ 42.50', successRate: '98.2%' },
  { date: '2026-04-21', type: '图片生成', count: 321, cost: '¥ 52.80', successRate: '97.5%' },
  { date: '2026-04-20', type: '视频生成', count: 89, cost: '¥ 124.60', successRate: '95.3%' },
  { date: '2026-04-19', type: '图片生成', count: 412, cost: '¥ 68.20', successRate: '98.7%' },
  { date: '2026-04-18', type: '视频生成', count: 124, cost: '¥ 176.40', successRate: '96.8%' }
]
</script>

<style scoped lang="scss">
.api-cost-page {
  width: 100%;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  padding: 24px;
  
  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .stat-title {
      font-size: 16px;
      font-weight: 500;
      color: $text-secondary;
      margin: 0;
    }

    .stat-icon {
      background: $primary-gradient;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .stat-value {
    font-size: 32px;
    font-weight: 700;
    margin: 0 0 8px 0;
  }

  .stat-trend {
    font-size: 14px;
    margin: 0;

    &.up {
      color: #10b981;
    }
    &.down {
      color: #ef4444;
    }
  }
}

.chart-card, .table-card {
  padding: 24px;
  margin-bottom: 20px;

  .chart-title, .table-title {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 20px 0;
  }

  .chart-placeholder {
    height: 300px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-tertiary;
    background: rgba(100, 108, 255, 0.05);
    border-radius: 8px;
  }
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-header-bg-color: rgba(100, 108, 255, 0.05);
  --el-table-border-color: rgba(100, 108, 255, 0.1);
  --el-table-text-color: $text-primary;
  --el-table-header-text-color: $text-primary;
}
</style>
