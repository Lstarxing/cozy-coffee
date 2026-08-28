<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑单品豆' : '新增单品豆'"
    width="640px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-width="110px">
      <el-form-item label="代码" required><el-input v-model="form.code" placeholder="ETH_WASHED_LIGHT" /></el-form-item>
      <div class="form-row">
        <el-form-item label="豆名（中）" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="豆名（英）"><el-input v-model="form.nameEn" /></el-form-item>
      </div>
      <el-form-item label="所属产区">
        <el-select v-model="form.originId" placeholder="选择产区" filterable style="width: 100%">
          <el-option v-for="o in origins" :key="o.id" :value="o.id" :label="`${o.countryZh || o.country}（${o.code}）`" />
        </el-select>
      </el-form-item>
      <div class="form-row">
        <el-form-item label="海拔"><el-input v-model="form.altitude" placeholder="1,800-2,200m" /></el-form-item>
        <el-form-item label="处理法"><el-input v-model="form.processing" placeholder="Washed" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="品种"><el-input v-model="form.variety" placeholder="Heirloom" /></el-form-item>
        <el-form-item label="烘焙度"><el-input v-model="form.roast" placeholder="Light / Medium-Dark" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="风味"><el-input v-model="form.flavorNotes" placeholder="花香 · 柑橘 · 蜂蜜" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="醇厚度"><el-input v-model="form.body" placeholder="Full / Smooth" /></el-form-item>
        <el-form-item label="酸度"><el-input v-model="form.acidity" placeholder="Balanced / Bright" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="角色"><el-input v-model="form.role" placeholder="浓缩基底 / 花香层次" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :precision="0" /></el-form-item>
      </div>
      <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 160px">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button size="large" @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" size="large" :loading="saving" @click="$emit('confirm')">
          <el-icon class="el-icon--left"><Check /></el-icon>保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { Check } from '@element-plus/icons-vue'

defineProps({
  visible: { type: Boolean, default: false },
  form: { type: Object, default: () => ({}) },
  origins: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})
defineEmits(['update:visible', 'confirm'])
</script>

<style scoped>
.form-row { display: flex; gap: 16px; }
.form-row :deep(.el-form-item) { flex: 1; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
</style>
