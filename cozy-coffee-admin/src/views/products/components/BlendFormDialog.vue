<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑拼配豆' : '新增拼配豆'"
    width="680px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-width="110px">
      <el-form-item label="代码" required><el-input v-model="form.code" placeholder="COZY_HOUSE" /></el-form-item>
      <div class="form-row">
        <el-form-item label="拼配名（中）" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="拼配名（英）"><el-input v-model="form.nameEn" /></el-form-item>
      </div>

      <el-form-item label="拼配比例" required>
        <div class="composition-editor">
          <div v-for="(row, idx) in form.composition" :key="idx" class="composition-row">
            <el-select v-model="row.beanId" placeholder="选择单品豆" filterable style="flex: 1">
              <el-option v-for="b in activeBeans" :key="b.id" :value="b.id" :label="`${b.name}（${b.code}）`" />
            </el-select>
            <el-input-number v-model="row.ratio" :min="1" :max="100" :precision="0" controls-position="right" style="width: 110px" />
            <span class="ratio-unit">%</span>
            <el-button link type="danger" size="small" @click="removeRow(idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button size="small" type="primary" plain @click="addRow">
            <el-icon class="el-icon--left"><Plus /></el-icon>添加拼配项
          </el-button>
          <div class="composition-sum" :class="{ error: totalRatio !== 100 }">
            合计 {{ totalRatio }}%（需等于 100）{{ totalRatio !== 100 ? '⚠️ 比例未满' : '✓' }}
          </div>
        </div>
      </el-form-item>

      <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      <div class="form-row">
        <el-form-item label="烘焙度"><el-input v-model="form.roast" placeholder="Medium" /></el-form-item>
        <el-form-item label="风味"><el-input v-model="form.flavorNotes" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="醇厚度"><el-input v-model="form.body" /></el-form-item>
        <el-form-item label="酸度"><el-input v-model="form.acidity" /></el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :precision="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
      </div>
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
import { computed } from 'vue'
import { Plus, Delete, Check } from '@element-plus/icons-vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  form: { type: Object, default: () => ({}) },
  beans: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})
defineEmits(['update:visible', 'confirm'])

const activeBeans = computed(() => props.beans.filter(b => b.status === 'active'))
const totalRatio = computed(() =>
  (props.form.composition || []).reduce((sum, r) => sum + Number(r.ratio || 0), 0))

function addRow() {
  if (!Array.isArray(props.form.composition)) props.form.composition = []
  props.form.composition.push({ beanId: null, ratio: null })
}
function removeRow(idx) {
  props.form.composition.splice(idx, 1)
}
</script>

<style scoped>
.form-row { display: flex; gap: 16px; }
.form-row :deep(.el-form-item) { flex: 1; }
.composition-editor { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.composition-row { display: flex; align-items: center; gap: 8px; }
.ratio-unit { color: #606266; font-size: 13px; }
.composition-sum { font-size: 12px; color: #166534; }
.composition-sum.error { color: #E65100; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
</style>
