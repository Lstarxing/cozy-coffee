<template>
  <div class="image-upload-section">
    <el-upload
      class="avatar-uploader"
      :show-file-list="false"
      :before-upload="beforeImageUpload"
      :http-request="handleImageUpload"
      accept="image/*"
    >
      <img v-if="modelValue" :src="getImageUrl(modelValue)" class="avatar" />
      <div v-else class="avatar-uploader-placeholder">
        <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
        <div class="upload-text">点击上传</div>
      </div>
    </el-upload>
    <div class="url-input-wrapper">
      <el-input
        :model-value="modelValue"
        placeholder="或输入图片 URL"
        size="small"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <template #prefix>
          <el-icon><Link /></el-icon>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { Plus, Link } from '@element-plus/icons-vue'
import { uploadImage } from '@/api'
import { getImageUrl } from '@/utils/image'

const props = defineProps({
  modelValue: { type: String, default: '' },
  uploadType: { type: String, default: 'products' } // coffee / points / products
})

const emit = defineEmits(['update:modelValue'])

const beforeImageUpload = (file) => {
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

const handleImageUpload = async (opt) => {
  try {
    const res = await uploadImage(opt.file, props.uploadType)
    if (res.data?.url) {
      emit('update:modelValue', res.data.url)
      ElMessage.success('上传成功')
    } else {
      ElMessage.error('上传成功但未返回图片 URL')
    }
  } catch (e) {
    console.error('图片上传失败:', e)
    ElMessage.error('上传失败: ' + (e.message || '未知错误'))
  }
}
</script>

<style scoped lang="scss">
.image-upload-section {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  .url-input-wrapper {
    flex: 1;
  }
}

.avatar-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  background: #fafafa;

  &:hover {
    border-color: var(--el-color-primary);
    background: #f5f7fa;
  }

  .avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.avatar-uploader-icon {
  font-size: 32px;
  color: #8c939d;
}

.upload-text {
  font-size: 12px;
  color: #909399;
}
</style>
