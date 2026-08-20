<!--
  地址编辑页 - 对齐 prototype/address-edit.html：表单卡 + 性别选择 + 保存；编辑态回填 gender
-->
<template>
  <view class="edit-page">
    <!-- 表单卡 -->
    <view class="form-card">
      <view class="form-row">
        <text class="form-label">联系人</text>
        <input v-model="form.name" placeholder="请填写收货人姓名" placeholder-class="form-placeholder" class="form-input" />
        <view class="gender-radios">
          <view class="gender-radio" :class="{ on: form.gender === 'MALE' }" @click="form.gender = 'MALE'">
            <view class="radio-dot" />
            <text>先生</text>
          </view>
          <view class="gender-radio" :class="{ on: form.gender === 'FEMALE' }" @click="form.gender = 'FEMALE'">
            <view class="radio-dot" />
            <text>女士</text>
          </view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input v-model="form.phone" type="number" maxlength="11" placeholder="请填写收货手机号" placeholder-class="form-placeholder" class="form-input" />
      </view>
      <picker mode="multiSelector" :range="regionRange" :value="regionIndexes" @change="onRegionChange" @columnchange="onRegionColumnChange">
        <view class="form-row tap">
          <text class="form-label">收货地址</text>
          <text class="form-value" :class="{ filled: form.region }">{{ form.region || '点击选择' }}</text>
          <text class="form-arrow">›</text>
        </view>
      </picker>
      <view class="form-row">
        <text class="form-label">门牌号</text>
        <input v-model="form.detail" placeholder="例：A座8102室" placeholder-class="form-placeholder" class="form-input" />
      </view>
      <view class="form-row">
        <text class="form-label">标签</text>
        <view class="label-options">
          <view class="label-option" :class="{ selected: form.label === 'HOME' }" @click="form.label = 'HOME'">家</view>
          <view class="label-option" :class="{ selected: form.label === 'COMPANY' }" @click="form.label = 'COMPANY'">公司</view>
          <view class="label-option" :class="{ selected: form.label === 'SCHOOL' }" @click="form.label = 'SCHOOL'">学校</view>
        </view>
      </view>
      <view class="form-row tap" @click="form.isDefault = !form.isDefault">
        <text class="form-label">设为默认</text>
        <view class="form-check" :class="{ on: form.isDefault }">
          <CozyIcon v-if="form.isDefault" name="check" :size="20" color="#fff" />
        </view>
      </view>
    </view>

    <view class="save-btn" @click="save">保存</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { post, put } from '@/api/request'
import chinaRegions from '@/data/china-regions.json'
import CozyIcon from '@/components/CozyIcon.vue'

const createAddress = (data) => post('/member/addresses', data)
const updateAddress = (id, data) => put(`/member/addresses/${id}`, data)

const isEditing = ref(false)
const editingId = ref(null)
const provinceCode = ref('')
const cityCode = ref('')
const regionIndexes = ref([0, 0, 0])
const form = ref({ name: '', gender: 'MALE', label: 'HOME', phone: '', region: '', detail: '', isDefault: false })

// 三级联动：省 → 市 → 区/县
const provinces = computed(() => Object.entries(chinaRegions['86'] || {}).map(([code, name]) => ({ code, name })))
const cities = computed(() => Object.entries(provinceCode.value ? (chinaRegions[provinceCode.value] || {}) : {}).map(([code, name]) => ({ code, name })))
const districts = computed(() => Object.entries(cityCode.value ? (chinaRegions[cityCode.value] || {}) : {}).map(([code, name]) => ({ code, name })))
const regionRange = computed(() => [
  provinces.value.map(i => i.name),
  cities.value.map(i => i.name),
  districts.value.map(i => i.name)
])

onLoad((options) => {
  if (options?.id) {
    isEditing.value = true
    editingId.value = Number(options.id)
    uni.setNavigationBarTitle({ title: '编辑地址' })
    const item = uni.getStorageSync('cozy_edit_address')
    uni.removeStorageSync('cozy_edit_address')
    if (item) {
      form.value.name = item.name || item.receiverName || ''
      form.value.gender = item.gender || 'MALE'
      form.value.label = item.label || 'HOME'
      form.value.phone = item.phone || item.receiverPhone || ''
      form.value.detail = item.detail || item.detailAddress || ''
      form.value.isDefault = Boolean(item.isDefault)
      const province = item.province || ''
      const city = item.city || ''
      const district = item.district || ''
      form.value.region = [province, city, district].filter(Boolean).join(' ')
      initRegion(province, city, district)
    }
  } else {
    uni.setNavigationBarTitle({ title: '新增地址' })
  }
})

function initRegion(provinceName, cityName, districtName) {
  const pIdx = provinces.value.findIndex(i => i.name === provinceName)
  if (pIdx < 0) return
  provinceCode.value = provinces.value[pIdx].code
  const cIdx = cities.value.findIndex(i => i.name === cityName)
  const ci = cIdx >= 0 ? cIdx : 0
  if (cIdx >= 0) cityCode.value = cities.value[cIdx].code
  const dIdx = cIdx >= 0 ? districts.value.findIndex(i => i.name === districtName) : -1
  regionIndexes.value = [pIdx, ci, dIdx >= 0 ? dIdx : 0]
}

function onRegionColumnChange(e) {
  const { column, value } = e.detail
  if (column === 0) {
    provinceCode.value = provinces.value[value]?.code || ''
    cityCode.value = ''
    // 直辖市等中间级仅一项（如北京→市辖区）时自动选中，让区县列直接可用
    if (cities.value.length === 1) {
      cityCode.value = cities.value[0].code
    }
    regionIndexes.value = [value, 0, 0]
  } else if (column === 1) {
    cityCode.value = cities.value[value]?.code || ''
    regionIndexes.value = [regionIndexes.value[0], value, 0]
  }
}

function onRegionChange(e) {
  const [pi, ci, di] = e.detail.value
  const p = provinces.value[pi]
  const c = cities.value[ci]
  const d = districts.value[di]
  form.value.region = [p?.name, c?.name, d?.name].filter(Boolean).join(' ')
  regionIndexes.value = [pi, ci, di]
}

const save = async () => {
  const { name, phone, region, detail } = form.value
  if (!name.trim() || !phone.trim() || !region || !detail.trim()) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (!/^1\d{10}$/.test(phone.trim())) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }

  const parts = region.split(/\s+/).filter(Boolean)
  const data = {
    receiverName: name.trim(),
    gender: form.value.gender,
    label: form.value.label,
    receiverPhone: phone.trim(),
    province: parts[0] || '',
    city: parts[1] || '',
    district: parts[2] || '',
    detailAddress: detail.trim(),
    isDefault: form.value.isDefault
  }

  try {
    const res = isEditing.value
      ? await updateAddress(editingId.value, data)
      : await createAddress(data)
    if (res.code === 200) {
      uni.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
    }
  } catch (e) {
    console.error('保存地址失败', e)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.edit-page {
  min-height: 100vh;
  padding: 24rpx 32rpx 120rpx;
  background: $cozy-bg;
}

/* ── 表单卡：标签左 · 控件右 ── */
.form-card {
  border-radius: 28rpx;
  background: $bg-white;
  border: 1rpx solid $cozy-border;
  overflow: hidden;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-height: 96rpx;
  padding: 0 32rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.form-row:last-child { border-bottom: 0; }
.form-row.tap { cursor: pointer; }
.form-row.tap:active { opacity: .7; }
.form-label {
  flex: none;
  width: 116rpx;
  font-size: 24rpx;
  color: $cozy-ink;
}
.form-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 24rpx;
  color: $cozy-ink;
  height: 40rpx;
  line-height: 40rpx;
  padding: 0;
}
.form-placeholder { color: $cozy-placeholder; }
.form-value {
  margin-left: auto;
  font-size: 24rpx;
  color: $cozy-placeholder;
}
.form-value.filled { color: $cozy-ink; }
.form-arrow {
  font-size: 28rpx;
  color: $cozy-placeholder;
  line-height: 1;
}
.form-check {
  margin-left: auto;
  width: 40rpx;
  height: 40rpx;
  border-radius: 8rpx;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.form-check.on { background: $cozy-ink; border-color: $cozy-ink; }

/* 联系人右侧：单选 radio（先生/女士） */
.gender-radios { flex: none; display: flex; align-items: center; gap: 24rpx; }
.gender-radio {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: $cozy-muted;

  &.on { color: $cozy-ink; }
}
.radio-dot {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  border: 2rpx solid $cozy-border;
  background: $bg-white;
  box-sizing: border-box;
}
.gender-radio.on .radio-dot {
  border-color: $cozy-ink;
  background: radial-gradient(circle, $cozy-ink 0, $cozy-ink 7rpx, $bg-white 7rpx);
}

/* 标签：小巧胶囊，置于设为默认上一行 */
.label-options { margin-left: auto; display: flex; gap: 12rpx; }
.label-option {
  min-width: 84rpx;
  height: 48rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid $cozy-border;
  border-radius: 999rpx;
  background: $bg-white;
  color: $cozy-muted;
  font-size: 22rpx;

  &.selected {
    background: $cozy-surface;
    border-color: $cozy-ink;
    color: $cozy-ink;
    font-weight: 600;
  }
}

/* ── 保存 ── */
.save-btn {
  margin-top: 48rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;

  &:active { opacity: .85; }
}
</style>
