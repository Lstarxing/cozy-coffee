import { ref, computed } from 'vue'
import {
  getAddresses,
  createAddress as createAddressApi,
  updateAddress as updateAddressApi,
  deleteAddress as deleteAddressApi,
  setDefaultAddress as setDefaultAddressApi
} from '@/api/member'

export function useAddresses() {
  const addresses = ref([])
  const selectedAddressId = ref('')

  const selectedAddress = computed(() => {
    if (!selectedAddressId.value) return null
    return addresses.value.find(addr => addr.id === selectedAddressId.value) || null
  })

  async function loadAddresses() {
    try {
      const data = await getAddresses()
      if (data && data.data) {
        addresses.value = data.data || []
        if (!selectedAddressId.value) {
          const defaultAddr = addresses.value.find(a => a.isDefault)
          if (defaultAddr) selectedAddressId.value = defaultAddr.id
        }
      }
    } catch (error) {
      console.error('加载地址失败:', error)
    }
  }

  async function createAddress(data) {
    await createAddressApi(data)
    await loadAddresses()
  }

  async function updateAddress(id, data) {
    await updateAddressApi(id, data)
    await loadAddresses()
  }

  async function removeAddress(id) {
    await deleteAddressApi(id)
    await loadAddresses()
  }

  async function setDefaultAddress(id) {
    await setDefaultAddressApi(id)
    await loadAddresses()
  }

  return {
    addresses,
    selectedAddressId,
    selectedAddress,
    loadAddresses,
    createAddress,
    updateAddress,
    removeAddress,
    setDefaultAddress
  }
}
