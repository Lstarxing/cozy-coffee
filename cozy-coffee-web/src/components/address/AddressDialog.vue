<template>
  <div class="address-modal" v-if="modelValue" @click.self="closeModal">
    <div class="modal-content address-modal-content">
      <h3>{{ isEditing ? '编辑收货地址' : '添加收货地址' }}</h3>

      <div class="form-row">
        <div class="form-item">
          <label>收货人姓名 <span class="required">*</span></label>
          <input v-model="newAddress.receiverName" type="text" placeholder="请输入收货人姓名" />
        </div>
        <div class="form-item">
          <label>联系电话 <span class="required">*</span></label>
          <input v-model="newAddress.receiverPhone" type="text" placeholder="请输入手机号码" />
        </div>
      </div>

      <div class="form-row region-row">
        <div class="form-item">
          <label>省份 <span class="required">*</span></label>
          <select v-model="selectedProvinceCode" class="region-select">
            <option value="">请选择省份</option>
            <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>城市 <span class="required">*</span></label>
          <select v-model="selectedCityCode" class="region-select" :disabled="!selectedProvinceCode">
            <option value="">请选择城市</option>
            <option v-for="c in cities" :key="c.code" :value="c.code">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>区/县</label>
          <select v-model="selectedDistrictCode" class="region-select" :disabled="!selectedCityCode">
            <option value="">请选择区县</option>
            <option v-for="d in districts" :key="d.code" :value="d.code">{{ d.name }}</option>
          </select>
        </div>
      </div>

      <div v-if="isEditing && newAddress.province" class="current-region-hint">
        当前地址: {{ newAddress.province }} {{ newAddress.city }} {{ newAddress.district }}
        <span class="hint-text">（如需更改请重新选择）</span>
      </div>

      <div class="form-item full-width">
        <label>详细地址 <span class="required">*</span></label>
        <input v-model="newAddress.detailAddress" type="text" placeholder="请输入街道、门牌号等详细信息" />
      </div>

      <div class="form-item checkbox">
        <label>
          <input type="checkbox" v-model="newAddress.isDefault" />
          设为默认收货地址
        </label>
      </div>

      <div class="modal-actions">
        <button @click="closeModal" class="cancel-btn">取消</button>
        <button @click="saveAddress" class="confirm-btn">{{ isEditing ? '保存修改' : '确认添加' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import chinaRegions from '@/data/china-regions.json'

const props = defineProps({
  modelValue: Boolean,
  initialData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'saved', 'close'])

const newAddress = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const selectedProvinceCode = ref('')
const selectedCityCode = ref('')
const selectedDistrictCode = ref('')

const isEditing = computed(() => !!props.initialData && !!props.initialData.id)

// Regions
const provinces = computed(() => {
  const list = chinaRegions['86'] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

const cities = computed(() => {
  if (!selectedProvinceCode.value) return []
  const list = chinaRegions[selectedProvinceCode.value] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

const districts = computed(() => {
  if (!selectedCityCode.value) return []
  const list = chinaRegions[selectedCityCode.value] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

// Reset Logic
watch(selectedProvinceCode, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    selectedCityCode.value = ''
    selectedDistrictCode.value = ''
  }
})

watch(selectedCityCode, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    selectedDistrictCode.value = ''
  }
})

// Initialize from props
watch(() => props.modelValue, (val) => {
  if (val) {
    if (props.initialData) {
      const addr = props.initialData
      newAddress.value = {
        receiverName: addr.receiverName || addr.contactName || '',
        receiverPhone: addr.receiverPhone || addr.phone || '',
        province: addr.province || '',
        city: addr.city || '',
        district: addr.district || '',
        detailAddress: addr.detailAddress || '',
        isDefault: !!addr.isDefault
      }
      // Reset selectors as reverse lookup is complex
      selectedProvinceCode.value = ''
      selectedCityCode.value = ''
      selectedDistrictCode.value = ''
    } else {
      // Create mode
      newAddress.value = {
        receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false
      }
      selectedProvinceCode.value = ''
      selectedCityCode.value = ''
      selectedDistrictCode.value = ''
    }
  }
})

const getRegionName = (list, code) => list.find(i => i.code === code)?.name || ''

const saveAddress = async () => {
  const provinceName = getRegionName(provinces.value, selectedProvinceCode.value) || newAddress.value.province
  const cityName = getRegionName(cities.value, selectedCityCode.value) || newAddress.value.city
  const districtName = getRegionName(districts.value, selectedDistrictCode.value) || newAddress.value.district

  if (!newAddress.value.receiverName || !newAddress.value.receiverPhone ||
    !provinceName || !cityName || !newAddress.value.detailAddress) {
    ElMessage.warning('请填写完整的地址信息')
    return
  }

  // 构建 payload，映射字段以兼容后端
  const payload = {
    ...newAddress.value,
    contactName: newAddress.value.receiverName,
    phone: newAddress.value.receiverPhone,
    province: provinceName,
    city: cityName,
    district: districtName
  }
  // 移除为了显示兼容而可能多余的字段（视情况而定，这里保留也没事）

  try {
    const token = localStorage.getItem('token')
    const url = isEditing.value 
      ? `http://localhost:8080/api/member/addresses/${props.initialData.id}`
      : `http://localhost:8080/api/member/addresses`
    const method = isEditing.value ? 'PUT' : 'POST'

    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    })
    const data = await res.json()
    if (data.success) {
      ElMessage.success(isEditing.value ? '修改成功' : '添加成功')
      closeModal()
      emit('saved')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('系统错误')
  }
}

const closeModal = () => {
  emit('update:modelValue', false)
  emit('close')
}
</script>

<style scoped>
/* 模态框基础样式 (参考 Member.vue 风格) */
.address-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000; /* 高于 Sidebar */
  padding: 20px; /* 确保模态框不会紧贴边缘 */
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 30px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh; /* 限制最大高度 */
  overflow-y: auto; /* 内容过多时可滚动 */
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative; /* 确保下拉框相对定位正确 */
}

.address-modal h3 {
  margin: 0 0 24px 0;
  font-size: 20px;
  color: #3E2723;
  text-align: center;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.region-row {
  position: relative;
  z-index: 10; /* 确保下拉框在其他元素上方 */
  margin-bottom: 24px; /* 增加底部间距，防止下拉框被遮挡 */
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative; /* 确保下拉框相对父元素定位 */
}

.form-item label {
  font-size: 13px;
  color: #5D4037;
  font-weight: 600;
}

.required {
  color: #D32F2F;
}

.form-item input,
.form-item select {
  padding: 10px 14px;
  border: 1px solid #E0E0E0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  background: #FAFAFA;
}

.form-item input:focus,
.form-item select:focus {
  border-color: #8D6E63;
  background: white;
  box-shadow: 0 0 0 3px rgba(141, 110, 99, 0.1);
}

.full-width {
  width: 100%;
  margin-bottom: 16px;
}

.checkbox {
  flex-direction: row;
  align-items: center;
}

.checkbox label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 400;
  color: #666;
}

.checkbox input {
  width: 16px;
  height: 16px;
  margin: 0;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn {
  flex: 1;
  padding: 12px;
  background: #F5F5F5;
  border: none;
  border-radius: 12px;
  color: #666;
  font-weight: 600;
  cursor: pointer;
}

.confirm-btn {
  flex: 2;
  padding: 12px;
  background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
}

.current-region-hint {
  font-size: 12px;
  color: #8D6E63;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #EFEBE9;
  border-radius: 8px;
}

.hint-text {
  color: #999;
}
</style>
