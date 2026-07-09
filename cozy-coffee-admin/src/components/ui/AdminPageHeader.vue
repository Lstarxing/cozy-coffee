<template>
  <div class="admin-page-header">
    <!-- 面包屑导航 -->
    <el-breadcrumb v-if="breadcrumbs.length" class="breadcrumb" separator="/">
      <el-breadcrumb-item 
        v-for="(crumb, idx) in breadcrumbs" 
        :key="idx"
        :to="crumb.path"
      >
        {{ crumb.label }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <div class="header-content">
      <div class="header-left">
        <div class="title-row">
          <el-button 
            v-if="showBack || backPath" 
            :icon="ArrowLeft" 
            circle 
            size="small" 
            class="back-btn"
            @click="handleBack"
          />
          <h1 class="page-title">{{ title }}</h1>
        </div>
        <span v-if="subtitle" class="page-subtitle">{{ subtitle }}</span>
      </div>
      <div class="header-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    <div v-if="$slots.extra" class="header-extra">
      <slot name="extra"></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  backPath: {
    type: String,
    default: ''
  },
  // 显示返回按钮（使用 router.back()）
  showBack: {
    type: Boolean,
    default: false
  },
  // 面包屑配置 [{ label, path }]
  breadcrumbs: {
    type: Array,
    default: () => []
  }
})

const router = useRouter()

const handleBack = () => {
  if (props.backPath) {
    router.push(props.backPath)
  } else {
    router.back()
  }
}
</script>

<style scoped lang="scss">
.admin-page-header {
  margin-bottom: 24px;

  .breadcrumb {
    margin-bottom: 16px;
  }

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .back-btn {
    border: 1px solid #E5E7EB;
  }

  .page-title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #111827;
    line-height: 28px;
  }

  .page-subtitle {
    font-size: 14px;
    color: #6B7280;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .header-extra {
    margin-top: 16px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
}
</style>
