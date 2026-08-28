<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑产区' : '新增产区'"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" label-width="110px">
      <el-form-item label="代码" required><el-input v-model="form.code" placeholder="ETHIOPIA" /></el-form-item>
      <el-form-item label="国家（英）" required><el-input v-model="form.country" placeholder="Ethiopia" /></el-form-item>
      <el-form-item label="国家（中）"><el-input v-model="form.countryZh" placeholder="埃塞俄比亚" /></el-form-item>
      <el-form-item label="产区/子区域"><el-input v-model="form.region" placeholder="Yirgacheffe" /></el-form-item>
      <el-form-item label="典型气质"><el-input v-model="form.typicalCharacter" placeholder="花香 · 明亮" /></el-form-item>
      <el-form-item label="来源故事"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :precision="0" /></el-form-item>
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
  saving: { type: Boolean, default: false }
})
defineEmits(['update:visible', 'confirm'])
</script>

<style scoped>
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
</style>
