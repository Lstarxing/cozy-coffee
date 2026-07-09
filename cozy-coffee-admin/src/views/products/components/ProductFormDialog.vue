<template>
  <el-dialog
    :model-value="visible"
    :close-on-click-modal="false"
    v-bind="$attrs"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form v-bind="$attrs" :model="form" label-width="120px" label-position="left">
      <slot />
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button size="large" @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" size="large" @click="$emit('confirm')">
          <el-icon class="el-icon--left"><Check /></el-icon>
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { Check } from '@element-plus/icons-vue'

defineProps({
  visible: { type: Boolean, default: false },
  form: { type: Object, default: () => ({}) }
})

defineEmits(['update:visible', 'confirm'])
</script>

<style scoped lang="scss">
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
