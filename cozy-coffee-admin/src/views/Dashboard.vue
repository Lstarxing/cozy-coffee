<template>
  <div class="dashboard-page">
    <AdminPageHeader title="控制台" subtitle="运营数据总览">
      <template #extra>
        <div class="header-controls">
           <el-radio-group v-model="datePreset" size="small" @change="handlePresetChange">
              <el-radio-button value="today">今天</el-radio-button>
              <el-radio-button value="yesterday">昨日</el-radio-button>
              <el-radio-button value="week">近7天</el-radio-button>
              <el-radio-button value="month">近30天</el-radio-button>
           </el-radio-group>
           
           <el-date-picker 
             v-model="dateRange" 
             type="daterange" 
             range-separator="-" 
             start-placeholder="开始" 
             end-placeholder="结束" 
             size="small"
             value-format="YYYY-MM-DD"
             style="width: 200px"
             :clearable="false"
             @change="handleDateChange"
           />
           
           <el-divider direction="vertical" />
           <span class="last-updated">更新于 {{ lastUpdated }}</span>
           <el-button :icon="Refresh" circle size="small" @click="refreshData" :loading="loading" />
        </div>
      </template>
    </AdminPageHeader>

    <div class="dashboard-content" v-loading="loading && !stats.coffeeOrders"> <!-- Only block if init load -->
      
      <!-- KPI Tiles -->
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :lg="6" class="mb-mobile">
          <KpiTile 
            label="总营收" 
            :value="stats.coffeeRevenue" 
            currency 
            :icon="TrendCharts"
            to="/orders"
          >
             <template #footer><span class="text-gray">仅统计咖啡订单</span></template>
          </KpiTile>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6" class="mb-mobile">
           <KpiTile label="咖啡订单" :value="stats.coffeeOrders" :icon="CoffeeCup" to="/orders">
              <template #footer>笔</template>
           </KpiTile>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6" class="mb-mobile">
           <KpiTile label="兑换订单" :value="stats.redemptionOrders" :icon="Present" to="/redemptions">
              <template #footer>笔</template>
           </KpiTile>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6" class="mb-mobile">
           <KpiTile label="积分消耗" :value="stats.pointsSpent" :icon="Star">
              <template #footer>积分</template>
           </KpiTile>
        </el-col>
      </el-row>

      <!-- Charts Row 1 -->
      <section class="dashboard-section">
        <el-row :gutter="24">
          <el-col :span="16">
              <ChartCard title="营收与订单趋势" :body-height="320">
                <template #actions>
                    <el-radio-group v-model="trendGranularity" size="small" @change="loadTrend">
                      <el-radio-button value="day">按日</el-radio-button>
                      <el-radio-button value="hour">按时</el-radio-button>
                    </el-radio-group>
                </template>
                <TrendChart :data="trendData" />
              </ChartCard>
          </el-col>
          <el-col :span="8">
              <ChartCard title="订单状态分布" :body-height="320">
                  <template #actions>
                    <el-select v-model="distDomain" size="small" style="width: 100px" @change="loadDistribution">
                        <el-option label="咖啡" value="coffee" />
                        <el-option label="兑换" value="redemption" />
                    </el-select>
                  </template>
                  <StatusDonutChart :data="distributionData" />
              </ChartCard>
          </el-col>
        </el-row>
      </section>

      <!-- Charts Row 2 -->
      <section class="dashboard-section">
        <el-row :gutter="24">
          <el-col :span="12">
              <ChartCard title="商品排行 TOP10" :body-height="320">
                <template #actions>
                    <el-select v-model="rankDomain" size="small" style="width: 90px" @change="loadRank">
                        <el-option label="咖啡" value="coffee" />
                        <el-option label="兑换" value="redemption" />
                    </el-select>
                    <el-select v-model="rankMetric" size="small" style="width: 90px" @change="loadRank">
                        <el-option label="销量" value="count" />
                        <el-option label="金额" value="amount" v-if="rankDomain==='coffee'" />
                        <el-option label="积分" value="points" v-if="rankDomain==='redemption'" />
                    </el-select>
                </template>
                <RankBarChart :data="rankData" />
              </ChartCard>
          </el-col>
          <el-col :span="12">
              <el-card shadow="never" class="recent-card">
                <!-- Content handled by RecentActivity tabs -->
                <RecentActivity ref="recentRef" />
              </el-card>
          </el-col>
        </el-row>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import { Refresh, TrendCharts, CoffeeCup, Present, Star } from '@element-plus/icons-vue'
import { getDashboardStats, getAnalyticsTrend, getAnalyticsDistribution, getAnalyticsRank } from '../api'

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import KpiTile from '../components/dashboard/KpiTile.vue'
import ChartCard from '../components/dashboard/ChartCard.vue'
import TrendChart from '../components/dashboard/TrendChart.vue'
import StatusDonutChart from '../components/dashboard/StatusDonutChart.vue'
import RankBarChart from '../components/dashboard/RankBarChart.vue'
import RecentActivity from '../components/dashboard/RecentActivity.vue'

// State
const loading = ref(false)
const lastUpdated = ref('')
const datePreset = ref('today')
const dateRange = ref([]) // [start, end]

const stats = ref({ coffeeRevenue: 0, coffeeOrders: 0, redemptionOrders: 0, pointsSpent: 0 })
const trendData = ref([])
const trendGranularity = ref('hour')

const distributionData = ref([])
const distDomain = ref('coffee')

const rankData = ref([])
const rankDomain = ref('coffee')
const rankMetric = ref('count')

const recentRef = ref(null)

// Date High-Level Logic
const setPreset = (preset) => {
  const end = dayjs()
  let start = dayjs()
  
  if (preset === 'today') {
     start = dayjs()
     trendGranularity.value = 'hour' // Auto switch granularity
  } else if (preset === 'yesterday') {
     start = dayjs().subtract(1, 'day')
     trendGranularity.value = 'hour'
  } else if (preset === 'week') {
     start = dayjs().subtract(6, 'day')
     trendGranularity.value = 'day'
  } else if (preset === 'month') {
     start = dayjs().subtract(29, 'day')
     trendGranularity.value = 'day'
  }
  
  // Actually force the Yesterday case to be Start=Yesterday, End=Yesterday? 
  // Dashboard usually "Until Now" or specific range. "Yesterday" implies 00:00-23:59 of yesterday.
  if (preset === 'yesterday') {
      dateRange.value = [start.format('YYYY-MM-DD'), start.format('YYYY-MM-DD')]
  } else {
      dateRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
  }
}

const handlePresetChange = (val) => {
   setPreset(val)
   refreshData()
}

const handleDateChange = () => {
   datePreset.value = '' // clear preset
   refreshData()
}

// Data Fetching
const refreshData = () => {
   loadStats()
   loadTrend()
   loadDistribution()
   loadRank()
   recentRef.value?.loadData()
   lastUpdated.value = dayjs().format('HH:mm:ss')
}

const getParams = () => ({
  startDate: dateRange.value[0],
  endDate: dateRange.value[1]
})

const loadStats = async () => {
  console.log('[Dashboard] loadStats called, dateRange:', dateRange.value)
  try {
     const res = await getDashboardStats(dateRange.value[0], dateRange.value[1])
     console.log('[Dashboard] stats response:', res)
     stats.value = res.data || {}
  } catch (e) {
     console.error('[Dashboard] loadStats error:', e)
  }
}

const loadTrend = async () => {
  console.log('[Dashboard] loadTrend called')
  try {
     const res = await getAnalyticsTrend({
        ...getParams(),
        granularity: trendGranularity.value
     })
     console.log('[Dashboard] trend response:', res)
     trendData.value = res.data || []
  } catch (e) { console.error('[Dashboard] loadTrend error:', e) }
}

const loadDistribution = async () => {
  console.log('[Dashboard] loadDistribution called')
  try {
     const res = await getAnalyticsDistribution({
        ...getParams(),
        domain: distDomain.value
     })
     console.log('[Dashboard] distribution response:', res)
     distributionData.value = res.data || []
  } catch (e) { console.error('[Dashboard] loadDistribution error:', e) }
}

const loadRank = async () => {
   console.log('[Dashboard] loadRank called')
   try {
     const res = await getAnalyticsRank({
        ...getParams(),
        domain: rankDomain.value,
        metric: rankMetric.value
     })
     console.log('[Dashboard] rank response:', res)
     rankData.value = res.data || []
   } catch (e) { console.error('[Dashboard] loadRank error:', e) }
}

// Init
onMounted(() => {
   console.log('[Dashboard] onMounted triggered')
   setPreset('today')
   refreshData()
})
</script>

<style scoped lang="scss">
.dashboard-content {
   display: flex;
   flex-direction: column;
   gap: 24px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-updated {
  font-size: 12px;
  color: #9CA3AF;
}

.text-gray { color: #9CA3AF; font-size: 12px; }

@media (max-width: 992px) {
  .mb-mobile { margin-bottom: 24px; }
}

/* 关键：section 隔离，抵御 el-row gutter 负 margin 外溢 */
.dashboard-section {
  display: flow-root; /* 清除浮动并创建 BFC */
  position: relative;
  isolation: isolate; /* 隔离层叠上下文 */
}

.recent-card {
  height: 100%;
  border: 1px solid #E5E7EB;
  border-radius: 6px;
  
  :deep(.el-card__body) {
     padding: 0;
  }
}
</style>
