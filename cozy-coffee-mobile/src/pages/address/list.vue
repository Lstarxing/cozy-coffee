<!--
  地址管理页 - 收货地址列表与编辑
-->
<template>
  <view class="address-page">
    <!-- 地址列表 -->
    <view class="address-list" v-if="addresses.length > 0">
      <view class="address-card" v-for="item in addresses" :key="item.id">
        <view class="address-main" @click="selectAddress(item)">
          <view class="address-top">
            <text class="receiver">{{ item.name }}</text>
            <text class="phone">{{ item.phone }}</text>
            <view class="default-tag" v-if="item.isDefault">默认</view>
          </view>
          <text class="address-detail">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.detail }}</text>
        </view>
        <view class="address-actions">
          <view class="action-item" @click="setDefault(item)" v-if="!item.isDefault">
            <text class="action-icon">⭐</text>
            <text class="action-text">设为默认</text>
          </view>
          <view class="action-item" @click="editAddress(item)">
            <text class="action-icon">✏️</text>
            <text class="action-text">编辑</text>
          </view>
          <view class="action-item" @click="deleteAddress(item)">
            <text class="action-icon">🗑️</text>
            <text class="action-text">删除</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">📍</text>
      <text class="empty-text">暂无收货地址</text>
    </view>
    
    <!-- 添加按钮 -->
    <view class="add-btn-wrapper safe-area-bottom">
      <view class="add-btn" @click="addAddress">
        + 新增收货地址
      </view>
    </view>
    
    <!-- 编辑弹窗 -->
    <view class="modal-mask" v-if="showEditModal" @click="showEditModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ isEditing ? '编辑地址' : '新增地址' }}</text>
          <text class="modal-close" @click="showEditModal = false">×</text>
        </view>
        <view class="modal-body">
          <view class="form-item">
            <text class="form-label">收货人</text>
            <input v-model="editForm.name" placeholder="请输入收货人姓名" class="form-input" />
          </view>
          <view class="form-item">
            <text class="form-label">手机号</text>
            <input v-model="editForm.phone" type="number" placeholder="请输入手机号" maxlength="11" class="form-input" />
          </view>
          <view class="form-item">
            <text class="form-label">所在地区</text>
            <input v-model="editForm.region" placeholder="省市区" class="form-input" />
          </view>
          <view class="form-item">
            <text class="form-label">详细地址</text>
            <input v-model="editForm.detail" placeholder="街道门牌号等" class="form-input" />
          </view>
          <view class="form-item checkbox-item">
            <view class="checkbox" :class="{ checked: editForm.isDefault }" @click="editForm.isDefault = !editForm.isDefault">
              <text v-if="editForm.isDefault">✓</text>
            </view>
            <text class="checkbox-label">设为默认地址</text>
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn" @click="saveAddress">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post, del, put } from '@/api/request'

// API 定义
const getAddressList = () => get('/member/addresses')
const createAddress = (data) => post('/member/addresses', data)
const updateAddress = (id, data) => put(`/member/addresses/${id}`, data)
const deleteAddressApi = (id) => del(`/member/addresses/${id}`)
const setDefaultAddressApi = (id) => put(`/member/addresses/${id}/default`)

const addresses = ref([])
const showEditModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const editForm = ref({
  name: '',
  phone: '',
  region: '',
  detail: '',
  isDefault: false
})

onMounted(() => {
  loadAddresses()
})

const loadAddresses = async () => {
  try {
    const res = await getAddressList()
    if (res.code === 200 && res.data) {
      addresses.value = res.data.map(item => ({
        ...item,
        name: item.receiverName,
        phone: item.receiverPhone,
        detail: item.detailAddress,
        isDefault: item.isDefault
      }))
    }
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

const addAddress = () => {
  isEditing.value = false
  editingId.value = null
  editForm.value = { name: '', phone: '', region: '', detail: '', isDefault: false }
  showEditModal.value = true
}

const editAddress = (item) => {
  isEditing.value = true
  editingId.value = item.id
  editForm.value = {
    name: item.name,
    phone: item.phone,
    region: `${item.province || ''}${item.city || ''}${item.district || ''}`,
    detail: item.detail,
    isDefault: item.isDefault
  }
  showEditModal.value = true
}

const saveAddress = async () => {
  if (!editForm.value.name || !editForm.value.phone || !editForm.value.detail) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  
  // 简单解析 region (实际应用中应用 picker)
  const regionParts = editForm.value.region.split(/(省|市|区|县)/).filter(Boolean)
  const province = regionParts[0] || '北京市'
  const city = regionParts[2] || '北京市'
  const district = regionParts[4] || '海淀区'

  const data = {
    receiverName: editForm.value.name,
    receiverPhone: editForm.value.phone,
    province,
    city,
    district,
    detailAddress: editForm.value.detail,
    isDefault: editForm.value.isDefault
  }

  try {
    let res
    if (isEditing.value) {
      res = await updateAddress(editingId.value, data)
    } else {
      res = await createAddress(data)
    }
    
    if (res.code === 200) {
      uni.showToast({ title: '保存成功', icon: 'success' })
      showEditModal.value = false
      loadAddresses()
    }
  } catch (e) {
    console.error('保存地址失败', e)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const setDefault = async (item) => {
  try {
    const res = await setDefaultAddressApi(item.id)
    if (res.code === 200) {
      uni.showToast({ title: '设置成功', icon: 'success' })
      loadAddresses()
    }
  } catch (e) {
    console.error('设置默认地址失败', e)
  }
}

const deleteAddress = (item) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          const delRes = await deleteAddressApi(item.id)
          if (delRes.code === 200) {
            uni.showToast({ title: '删除成功', icon: 'success' })
            loadAddresses()
          }
        } catch (e) {
          console.error('删除地址失败', e)
        }
      }
    }
  })
}

const selectAddress = (item) => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    const prevPage = pages[pages.length - 2]
    if (prevPage.route === 'pages/order/confirm') {
      uni.$emit('addressSelected', item)
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 140rpx;
}

// 地址列表
.address-list {
  padding: $spacing-md;
}

.address-card {
  background: $bg-white;
  border-radius: $border-radius-md;
  margin-bottom: $spacing-md;
  overflow: hidden;
  
  .address-main {
    padding: $spacing-md;
    
    .address-top {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-sm;
      
      .receiver {
        font-size: $font-size-lg;
        font-weight: 600;
        margin-right: $spacing-md;
      }
      
      .phone {
        font-size: $font-size-md;
        color: $text-secondary;
      }
      
      .default-tag {
        margin-left: $spacing-sm;
        background: $primary-color;
        color: white;
        font-size: $font-size-xs;
        padding: 2rpx 12rpx;
        border-radius: 4rpx;
      }
    }
    
    .address-detail {
      font-size: $font-size-md;
      color: $text-secondary;
      line-height: 1.5;
    }
  }
  
  .address-actions {
    display: flex;
    border-top: 1rpx solid $border-color;
    
    .action-item {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: $spacing-sm 0;
      
      .action-icon {
        font-size: 28rpx;
        margin-right: $spacing-xs;
      }
      
      .action-text {
        font-size: $font-size-sm;
        color: $text-secondary;
      }
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
  
  .empty-icon {
    font-size: 120rpx;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    color: $text-placeholder;
  }
}

// 添加按钮
.add-btn-wrapper {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-md;
  background: $bg-white;
  
  .add-btn {
    background: $cozy-primary;
    color: white;
    text-align: center;
    padding: $spacing-md;
    border-radius: 44rpx;
    font-size: $font-size-md;
    font-weight: 600;
  }
}

// 弹窗
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.modal-content {
  width: 100%;
  background: $bg-white;
  border-radius: $border-radius-lg $border-radius-lg 0 0;
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md;
    border-bottom: 1rpx solid $border-color;
    
    .modal-title {
      font-size: $font-size-lg;
      font-weight: 600;
    }
    
    .modal-close {
      font-size: 48rpx;
      color: $text-placeholder;
    }
  }
  
  .modal-body {
    padding: $spacing-md;
    
    .form-item {
      margin-bottom: $spacing-md;
      
      .form-label {
        font-size: $font-size-sm;
        color: $text-secondary;
        display: block;
        margin-bottom: $spacing-xs;
      }
      
      .form-input {
        width: 100%;
        padding: $spacing-sm;
        background: $bg-gray;
        border-radius: $border-radius-sm;
        font-size: $font-size-md;
      }
      
      &.checkbox-item {
        display: flex;
        align-items: center;
        
        .checkbox {
          width: 36rpx;
          height: 36rpx;
          border: 2rpx solid $border-color;
          border-radius: 6rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: $spacing-sm;
          
          &.checked {
            background: $primary-color;
            border-color: $primary-color;
            color: white;
            font-size: 24rpx;
          }
        }
        
        .checkbox-label {
          font-size: $font-size-md;
          color: $text-primary;
        }
      }
    }
  }
  
  .modal-footer {
    padding: $spacing-md;
    
    .modal-btn {
      background: $cozy-primary;
      color: white;
      text-align: center;
      padding: $spacing-md;
      border-radius: 44rpx;
      font-size: $font-size-md;
      font-weight: 600;
    }
  }
}
</style>
