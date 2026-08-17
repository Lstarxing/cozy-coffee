<template>
  <div class="profile-view">
    <header class="content-header">
      <h3>个人信息</h3>
    </header>

    <div class="form-container">
      <div class="form-group">
        <label>昵称</label>
        <div class="input-wrapper">
          <input v-if="isEditingNickname" v-model="editNickname" type="text" class="modern-input">
          <span v-else class="static-value">{{ userStore.userInfo?.nickname || '--' }}</span>
          <button v-if="!isEditingNickname" class="edit-btn" @click="startEditNickname">修改</button>
          <div v-else class="actions">
            <button class="save-btn" @click="saveNickname">保存</button>
            <button class="cancel-btn" @click="cancelEditNickname">取消</button>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label>手机号</label>
        <div class="input-wrapper">
          <input v-if="isEditingPhone" v-model="editPhone" type="text" class="modern-input">
          <span v-else class="static-value">{{ userStore.userInfo?.phone || '未绑定' }}</span>
          <button v-if="!isEditingPhone" class="edit-btn" @click="startEditPhone">修改</button>
          <div v-else class="actions">
            <button class="save-btn" @click="saveField('phone', editPhone)">保存</button>
            <button class="cancel-btn" @click="cancelEditPhone">取消</button>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label>生日</label>
        <div class="input-wrapper birthday-picker-wrapper">
          <el-date-picker
            v-if="isEditingBirthday"
            v-model="editBirthday"
            type="date"
            placeholder="选择您的生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
            class="premium-date-picker"
          />
          <span v-else class="static-value">
            <i v-if="userStore.userInfo?.birthday" class="icon-cake"></i>
            {{ userStore.userInfo?.birthday || '未设置' }}
          </span>
          <button v-if="!isEditingBirthday" class="edit-btn" @click="startEditBirthday">
            {{ userStore.userInfo?.birthday ? '修改' : '设置' }}
          </button>
          <div v-else class="actions">
            <button class="save-btn" @click="saveField('birthday', editBirthday)">保存</button>
            <button class="cancel-btn" @click="cancelEditBirthday">取消</button>
          </div>
        </div>
        <p v-if="userStore.userInfo?.birthday" class="field-hint">
          注意：生日每年仅限修改一次（下次可改：{{ formatExpireDate(userStore.userInfo?.nextBirthdayResetAt) }}）
        </p>
        <p v-else class="field-hint">生日月可获得会员等级专属生日权益包</p>
      </div>

      <div class="form-group">
        <label>邮箱</label>
        <div class="input-wrapper">
          <input v-if="isEditingEmail" v-model="editEmail" type="text" class="modern-input">
          <span v-else class="static-value">{{ userStore.userInfo?.email || '未绑定' }}</span>
          <button v-if="!isEditingEmail" class="edit-btn" @click="startEditEmail">修改</button>
          <div v-else class="actions">
            <button class="save-btn" @click="saveField('email', editEmail)">保存</button>
            <button class="cancel-btn" @click="cancelEditEmail">取消</button>
          </div>
        </div>
      </div>

      <!-- 邀请码相关 -->
      <div class="form-group">
        <label>我的邀请码</label>
        <div class="input-wrapper invite-wrapper">
          <span class="invite-code-text">{{ userStore.userInfo?.inviteCode || '生成中...' }}</span>
          <button class="copy-code-btn" @click="copyInviteCode">复制</button>
          <span class="invite-tip">邀请新用户注册首单后即可获得买一送一券</span>
        </div>
      </div>

      <div v-if="!userStore.userInfo?.hasAppliedInviteCode" class="form-group">
        <label>好友邀请码</label>
        <div class="input-wrapper">
          <input
v-model="inputInviteCode" type="text" class="modern-input invite-input" placeholder="输入8位邀请码"
            maxlength="8" @input="inputInviteCode = inputInviteCode.toUpperCase()">
          <button
class="save-btn verify-btn" :disabled="isApplyingInviteCode || inputInviteCode.length < 8"
            @click="applyInviteCode">
            {{ isApplyingInviteCode ? '验证中...' : '提交' }}
          </button>
        </div>
        <p class="field-hint">填写好友邀请码绑定关系，下单享优惠</p>
      </div>
      <div v-else class="form-group">
        <label>好友邀请码</label>
        <div class="input-wrapper">
          <span class="static-value completed">✅ 已填写完成</span>
        </div>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="password-section">
      <div class="section-header">
        <h4>修改密码</h4>
      </div>
      <div class="form-group">
        <label>原密码</label>
        <div class="input-wrapper">
          <input v-model="pwdOld" type="password" class="modern-input" placeholder="请输入当前密码">
        </div>
      </div>
      <div class="form-group">
        <label>新密码</label>
        <div class="input-wrapper">
          <input v-model="pwdNew" type="password" class="modern-input" placeholder="6-20 位新密码">
        </div>
      </div>
      <div class="form-group">
        <label>确认新密码</label>
        <div class="input-wrapper">
          <input v-model="pwdConfirm" type="password" class="modern-input" placeholder="再次输入新密码">
        </div>
      </div>
      <button class="password-save-btn" :disabled="changingPassword" @click="submitChangePassword">
        {{ changingPassword ? '修改中...' : '确认修改' }}
      </button>
      <p class="field-hint">修改成功后需重新登录</p>
    </div>

    <!-- 收货地址管理 -->
    <div class="address-management">
      <div class="section-header">
        <h4>收货地址</h4>
        <button class="add-btn" @click="openAddAddressModal">+ 添加地址</button>
      </div>
      <div v-if="addresses.length > 0" class="address-list">
        <div v-for="addr in addresses" :key="addr.id" class="address-card" :class="{ default: addr.isDefault }">
          <div class="addr-info">
            <div class="addr-header">
              <span class="receiver-name">{{ addr.receiverName }}</span>
              <span class="receiver-phone">{{ addr.receiverPhone }}</span>
              <span v-if="addr.isDefault" class="default-badge">默认</span>
            </div>
            <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district || '' }}{{ addr.detailAddress }}</p>
          </div>
          <div class="addr-actions">
            <button class="action-btn" @click="openEditAddressModal(addr)">编辑</button>
            <button v-if="!addr.isDefault" class="action-btn" @click="setDefaultAddress(addr.id)">设为默认</button>
            <button class="action-btn delete" @click="deleteAddress(addr.id)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="no-address">
        <p>暂无收货地址</p>
      </div>
    </div>

    <!-- 添加/编辑地址模态框 -->
    <div v-if="showAddAddressModal" class="address-modal" @click.self="closeAddressModal">
      <div class="modal-content address-modal-content">
        <h3>{{ isEditingAddress ? '编辑收货地址' : '添加收货地址' }}</h3>
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
        <div v-if="isEditingAddress && newAddress.province" class="current-region-hint">
          当前地址: {{ newAddress.province }} {{ newAddress.city }} {{ newAddress.district }}
          <span class="hint-text">（如需更改请重新选择）</span>
        </div>
        <div class="form-item full-width">
          <label>详细地址 <span class="required">*</span></label>
          <input v-model="newAddress.detailAddress" type="text" placeholder="请输入街道、门牌号等详细信息" />
        </div>
        <div class="form-item checkbox">
          <label>
            <input v-model="newAddress.isDefault" type="checkbox" />
            设为默认收货地址
          </label>
        </div>
        <div class="modal-actions">
          <button class="cancel-btn" @click="closeAddressModal">取消</button>
          <button class="confirm-btn" @click="saveAddress">{{ isEditingAddress ? '保存修改' : '确认添加' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import chinaRegions from '@/data/china-regions.json'
import { updateProfile, applyInviteCode as applyInviteCodeApi, changePassword } from '@/api/auth'
import { useAddresses } from '@/composables/useAddresses'

const userStore = useUserStore()

// Editing States
const isEditingNickname = ref(false)
const editNickname = ref('')
const isEditingPhone = ref(false)
const editPhone = ref('')
const isEditingEmail = ref(false)
const editEmail = ref('')
const isEditingBirthday = ref(false)
const editBirthday = ref('')

// Address state
const {
  addresses,
  loadAddresses,
  createAddress: createAddressApi,
  updateAddress: updateAddressApi,
  removeAddress: removeAddressApi,
  setDefaultAddress: setDefaultAddressApi
} = useAddresses()
const showAddAddressModal = ref(false)
const isEditingAddress = ref(false)
const editingAddressId = ref(null)

// Region cascade
const selectedProvinceCode = ref('')
const selectedCityCode = ref('')
const selectedDistrictCode = ref('')

// Invite code
const inputInviteCode = ref('')
const isApplyingInviteCode = ref(false)

// Password change
const pwdOld = ref('')
const pwdNew = ref('')
const pwdConfirm = ref('')
const changingPassword = ref(false)

const newAddress = ref({
  receiverName: '', receiverPhone: '',
  province: '', city: '', district: '',
  detailAddress: '', isDefault: false
})

// Region computed
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

watch(selectedProvinceCode, () => { selectedCityCode.value = ''; selectedDistrictCode.value = '' })
watch(selectedCityCode, () => { selectedDistrictCode.value = '' })

function startEditNickname() { editNickname.value = userStore.userInfo?.nickname || ''; isEditingNickname.value = true }
function cancelEditNickname() { isEditingNickname.value = false; editNickname.value = '' }
function startEditPhone() { editPhone.value = userStore.userInfo?.phone || ''; isEditingPhone.value = true }
function cancelEditPhone() { isEditingPhone.value = false; editPhone.value = '' }
function startEditEmail() { editEmail.value = userStore.userInfo?.email || ''; isEditingEmail.value = true }
function cancelEditEmail() { isEditingEmail.value = false; editEmail.value = '' }
function startEditBirthday() { editBirthday.value = userStore.userInfo?.birthday || '2000-01-01'; isEditingBirthday.value = true }
function cancelEditBirthday() { isEditingBirthday.value = false }

async function saveField(fieldName, fieldValue) {
  try {
    const body = {}; body[fieldName] = fieldValue
    await updateProfile(body)
    userStore.userInfo[fieldName] = fieldValue
    if (fieldName === 'nickname') isEditingNickname.value = false
    if (fieldName === 'phone') isEditingPhone.value = false
    if (fieldName === 'email') isEditingEmail.value = false
    if (fieldName === 'birthday') isEditingBirthday.value = false
    ElMessage.success('修改成功')
    setTimeout(async () => { try { await userStore.fetchMemberInfo() } catch (e) { console.warn(e) } }, 500)
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  }
}

function saveNickname() { return saveField('nickname', editNickname.value) }

function formatExpireDate(dateStr) {
  if (!dateStr) return '--'
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  } catch { return dateStr }
}

function openAddAddressModal() {
  isEditingAddress.value = false; editingAddressId.value = null
  selectedProvinceCode.value = ''; selectedCityCode.value = ''; selectedDistrictCode.value = ''
  newAddress.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false }
  showAddAddressModal.value = true
}

function openEditAddressModal(addr) {
  isEditingAddress.value = true; editingAddressId.value = addr.id
  newAddress.value = { receiverName: addr.receiverName, receiverPhone: addr.receiverPhone, province: addr.province, city: addr.city, district: addr.district || '', detailAddress: addr.detailAddress, isDefault: addr.isDefault }
  selectedProvinceCode.value = ''; selectedCityCode.value = ''; selectedDistrictCode.value = ''
  showAddAddressModal.value = true
}

function closeAddressModal() {
  showAddAddressModal.value = false; isEditingAddress.value = false; editingAddressId.value = null
  selectedProvinceCode.value = ''; selectedCityCode.value = ''; selectedDistrictCode.value = ''
  newAddress.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false }
}

function getSelectedProvinceName() { const found = provinces.value.find(p => p.code === selectedProvinceCode.value); return found?.name || '' }
function getSelectedCityName() { const found = cities.value.find(c => c.code === selectedCityCode.value); return found?.name || '' }
function getSelectedDistrictName() { const found = districts.value.find(d => d.code === selectedDistrictCode.value); return found?.name || '' }

async function saveAddress() {
  const provinceName = getSelectedProvinceName() || newAddress.value.province
  const cityName = getSelectedCityName() || newAddress.value.city
  const districtName = getSelectedDistrictName() || newAddress.value.district
  if (!newAddress.value.receiverName || !newAddress.value.receiverPhone || !provinceName || !cityName || !newAddress.value.detailAddress) {
    ElMessage.warning('请填写完整的地址信息'); return
  }
  const addressData = { ...newAddress.value, province: provinceName, city: cityName, district: districtName }
  try {
    if (isEditingAddress.value && editingAddressId.value) {
      await updateAddressApi(editingAddressId.value, addressData)
    } else {
      await createAddressApi(addressData)
    }
    ElMessage.success(isEditingAddress.value ? '修改成功' : '添加成功')
    closeAddressModal()
  } catch (error) { ElMessage.error(error.message || '操作失败') }
}

async function setDefaultAddress(addressId) {
  try { await setDefaultAddressApi(addressId); ElMessage.success('设置成功') }
  catch (error) { ElMessage.error(error.message || '设置失败') }
}

async function deleteAddress(addressId) {
  if (!confirm('确定要删除这个地址吗？')) return
  try { await removeAddressApi(addressId); ElMessage.success('删除成功') }
  catch (error) { ElMessage.error(error.message || '删除失败') }
}

async function copyInviteCode() {
  const code = userStore.userInfo?.inviteCode
  if (!code) { ElMessage.warning('邀请码加载中，请稍后再试'); return }
  try { await navigator.clipboard.writeText(code); ElMessage.success('邀请码已复制到剪贴板！') }
  catch (e) {
    const textarea = document.createElement('textarea')
    textarea.value = code; document.body.appendChild(textarea); textarea.select()
    document.execCommand('copy'); document.body.removeChild(textarea); ElMessage.success('邀请码已复制！')
  }
}

async function applyInviteCode() {
  if (inputInviteCode.value.length < 8) { ElMessage.warning('请输入完整的8位邀请码'); return }
  isApplyingInviteCode.value = true
  try {
    const data = await applyInviteCodeApi(inputInviteCode.value)
    ElMessage.success(data.message || '邀请码绑定成功！')
    inputInviteCode.value = ''
    if (userStore.userInfo) {
      if (!userStore.userInfo.hasAppliedInviteCode) {
        userStore.userInfo.hasAppliedInviteCode = true
      }
    }
    setTimeout(async () => {
      try { await userStore.fetchUserInfo(); await userStore.fetchMemberInfo() }
      catch (e) { console.warn('延迟刷新失败', e) }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.message || '网络错误，请重试')
  } finally { isApplyingInviteCode.value = false }
}

async function submitChangePassword() {
  if (!pwdOld.value) { ElMessage.warning('请输入原密码'); return }
  if (pwdNew.value.length < 6 || pwdNew.value.length > 20) { ElMessage.warning('新密码需为 6-20 位'); return }
  if (pwdNew.value === pwdOld.value) { ElMessage.warning('新密码不能与原密码相同'); return }
  if (pwdNew.value !== pwdConfirm.value) { ElMessage.warning('两次输入的新密码不一致'); return }
  changingPassword.value = true
  try {
    const data = await changePassword(pwdOld.value, pwdNew.value)
    ElMessage.success(data.message || '密码修改成功，请重新登录')
    pwdOld.value = ''; pwdNew.value = ''; pwdConfirm.value = ''
  } catch (error) {
    ElMessage.error(error.message || '修改失败，请重试')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  userStore.fetchUserInfo()
  loadAddresses()
})
</script>

<style scoped>
.profile-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 800px;
  margin: 0 auto;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.content-header h3 {
  font-size: 28px;
  font-weight: 300;
  color: #1a1a1a;
  margin: 0;
}

.form-container { max-width: 600px; }

.form-group { margin-bottom: 30px; }

.form-group label {
  display: block;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.static-value { font-size: 16px; color: #333; }
.static-value.completed { color: #52c41a; font-weight: 500; }

.modern-input {
  border: none; outline: none;
  font-size: 16px; width: 100%;
  background: transparent; color: #333;
  padding: 5px 0;
  user-select: text !important;
}

.edit-btn, .save-btn, .cancel-btn { background: none; border: none; font-size: 14px; cursor: pointer; padding: 5px 15px; }
.edit-btn { color: #C69C6D; }
.save-btn { color: #333; font-weight: 600; }
.cancel-btn { color: #999; }
.actions { display: flex; gap: 8px; }

.field-hint { font-size: 12px; color: #999; margin-top: 6px; }

.invite-wrapper { display: flex !important; align-items: center; gap: 12px; }
.invite-code-text { font-family: 'Courier New', monospace; font-size: 16px; font-weight: 600; color: #333; letter-spacing: 1px; background: #f5f5f5; padding: 6px 12px; border-radius: 6px; }
.copy-code-btn { background: #e0e0e0; color: #666; border: none; padding: 6px 12px; border-radius: 6px; font-size: 13px; cursor: pointer; }
.copy-code-btn:hover { background: #d0d0d0; color: #333; }
.invite-tip { color: #999; font-size: 12px; }

.verify-btn {
  margin-left: 12px;
  background: #d4a762; color: white;
  border: none; padding: 8px 20px; border-radius: 6px;
  cursor: pointer; white-space: nowrap; flex-shrink: 0;
  min-width: 80px; height: 42px;
  display: flex; align-items: center; justify-content: center;
}
.verify-btn:hover:not(:disabled) { background: #c39651; }
.verify-btn:disabled { background: #e0e0e0; cursor: not-allowed; }
.invite-input[type="text"] { text-transform: uppercase; letter-spacing: 1px; }

.birthday-picker-wrapper { display: flex !important; align-items: center; gap: 12px; }
.birthday-picker-wrapper .static-value { display: flex; align-items: center; gap: 8px; }
.birthday-picker-wrapper .icon-cake { color: #D4AF37; font-size: 18px; }

:deep(.premium-date-picker.el-input) { width: 220px; }
:deep(.premium-date-picker .el-input__wrapper) { background-color: #fdfaf3; border-radius: 8px; box-shadow: 0 0 0 1px #e0e0e0 inset; padding: 0 12px; height: 38px; transition: all 0.3s; }
:deep(.premium-date-picker .el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #C69C6D inset !important; background-color: #fff; }
:deep(.premium-date-picker .el-input__inner) { color: #2C1810; font-weight: 500; font-family: 'Helvetica Neue', Helvetica, sans-serif; }

/* Address Management */
.address-management { margin-top: 40px; background: white; border-radius: 12px; padding: 25px; box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.section-header h4 { margin: 0; font-size: 18px; font-weight: 500; }

/* Password Change */
.password-section { margin-top: 40px; background: white; border-radius: 12px; padding: 25px; box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02); max-width: 600px; }
.password-save-btn {
  margin-top: 8px;
  background: #d4a762; color: white;
  border: none; padding: 8px 24px; border-radius: 6px;
  cursor: pointer; min-width: 110px; height: 42px;
  font-size: 14px; font-weight: 500;
  display: flex; align-items: center; justify-content: center;
}
.password-save-btn:hover:not(:disabled) { background: #c39651; }
.password-save-btn:disabled { background: #e0e0e0; cursor: not-allowed; }

.add-btn { background: #C69C6D; color: white; border: none; padding: 8px 16px; border-radius: 20px; cursor: pointer; font-size: 14px; }
.add-btn:hover { background: #B88A5A; }

.address-list { display: flex; flex-direction: column; gap: 15px; }

.address-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 15px 20px; background: #f8f8f8; border-radius: 10px;
  border: 1px solid #eee; transition: all 0.3s;
}
.address-card.default { border-color: #C69C6D; background: #FFF9F0; }

.addr-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.receiver-name { font-weight: 600; font-size: 15px; }
.receiver-phone { color: #888; font-size: 14px; }
.default-badge { background: #C69C6D; color: white; padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.addr-detail { color: #666; font-size: 14px; margin: 0; }

.addr-actions { display: flex; gap: 10px; }

.action-btn {
  background: none; border: 1px solid #ddd;
  padding: 6px 12px; border-radius: 15px;
  font-size: 12px; cursor: pointer; color: #666;
}
.action-btn:hover { border-color: #C69C6D; color: #C69C6D; }
.action-btn.delete:hover { border-color: #e74c3c; color: #e74c3c; }

.no-address { text-align: center; padding: 24px; color: #999; }

/* Address Modal */
.address-modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}

.address-modal-content {
  width: 600px !important;
  max-width: 90vw; max-height: 85vh;
  overflow-y: auto; scrollbar-width: none; -ms-overflow-style: none;
}

.address-modal-content::-webkit-scrollbar { display: none; }

.address-modal-content h3 {
  margin: 0 0 25px 0; font-size: 20px; font-weight: 500;
  color: #333; text-align: center; padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.form-row { display: flex; gap: 15px; margin-bottom: 5px; }
.form-row .form-item { flex: 1; }
.region-row { margin-bottom: 15px; }
.region-row .form-item { min-width: 0; }

.region-select {
  width: 100%; padding: 12px 15px;
  border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; background-color: #fff; color: #333;
  cursor: pointer;
  appearance: none; -webkit-appearance: none; -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 12px center;
}
.region-select:focus { border-color: #C69C6D; outline: none; }
.region-select:disabled { background-color: #f5f5f5; color: #999; cursor: not-allowed; }

.required { color: #e74c3c; }

.current-region-hint {
  background: #FFF9F0; padding: 10px 15px; border-radius: 8px;
  margin-bottom: 15px; font-size: 13px; color: #666;
  border: 1px dashed #C69C6D;
}
.hint-text { color: #999; font-size: 12px; }

.form-item.full-width { width: 100%; }
.form-item { margin-bottom: 15px; }
.form-item label { display: block; margin-bottom: 8px; color: #555; font-size: 14px; font-weight: 500; }
.form-item input[type="text"] { width: 100%; padding: 12px 15px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.form-item input[type="text"]:focus { border-color: #C69C6D; outline: none; }
.form-item.checkbox label { display: flex; align-items: center; gap: 10px; cursor: pointer; font-weight: 400; }
.form-item.checkbox input { width: 18px; height: 18px; accent-color: #C69C6D; }

.modal-content {
  background: white; border-radius: 20px;
  padding: 32px; width: 90%;
  max-width: 480px; max-height: 80vh;
  overflow-y: auto; border: none;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-actions { display: flex; gap: 12px; justify-content: center; }
.modal-actions .cancel-btn { padding: 12px 32px; background: white; border: 1px solid #ddd; border-radius: 30px; cursor: pointer; }
.modal-actions .confirm-btn { padding: 12px 32px; background: #B8956B; color: white; border: none; border-radius: 30px; cursor: pointer; }
.modal-actions .confirm-btn:disabled { background: #ccc; cursor: not-allowed; }
</style>
