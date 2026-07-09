<template>
  <div class="products-page">
    <AdminPageHeader
      title="积分商品管理"
      subtitle="管理积分兑换权益商品"
    >
      <template #actions>
        <el-button type="primary" @click="showAddModal">
          <el-icon class="el-icon--left"><Plus /></el-icon>
          添加商品
        </el-button>
      </template>
    </AdminPageHeader>

    <AdminFilterBar @search="handleSearch" @reset="resetFilters">
      <el-form-item label="关键词">
        <el-input
          v-model="filters.keyword"
          placeholder="商品名称"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="filters.category" placeholder="全部" clearable style="width: 140px">
          <el-option label="周边商品" value="merchandise" />
          <el-option label="优惠券" value="coupon" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="上架" value="active" />
          <el-option label="下架" value="inactive" />
          <el-option label="售罄" value="sold_out" />
        </el-select>
      </el-form-item>
    </AdminFilterBar>

    <ProductTable
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :loading="loading"
      :data="paginatedData"
      :total="filteredData.length"
      :last-updated="lastUpdated"
      @refresh="loadData"
    >
      <template #productInfo>
        <el-table-column label="商品信息" min-width="240">
          <template #default="{ row }">
            <div class="product-info-cell">
              <el-image
                :src="getImageUrl(row.imageUrl)"
                class="product-thumb-rounded"
                fit="cover"
              >
                <template #error>
                  <div class="image-placeholder-rounded">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="product-meta-clean">
                <div class="product-title-bold">{{ row.name }}</div>
                <el-tooltip
                  v-if="row.description"
                  :content="row.description"
                  placement="top"
                  :show-after="500"
                >
                  <div class="product-desc-grey truncate">{{ row.description }}</div>
                </el-tooltip>
              </div>
            </div>
          </template>
        </el-table-column>
      </template>

      <template #price>
        <el-table-column label="所需积分" width="150" align="center">
          <template #default="{ row }">
            <span class="points-font">{{ row.pointsPrice }} 积分</span>
          </template>
        </el-table-column>
      </template>

      <template #category>
        <el-table-column label="分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :color="getCategoryColor(row.category)"
              effect="dark"
              class="category-pill"
              style="border: none;"
            >
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
      </template>

      <template #stock>
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            {{ row.stock }}
          </template>
        </el-table-column>
      </template>

      <template #status>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'active'"
              active-color="#13ce66"
              inactive-color="#ff4949"
              :loading="row.statusLoading"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
      </template>

      <template #actions>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-icons">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" class="icon-btn blue" @click="editProduct(row)">
                  <el-icon :size="18"><Edit /></el-icon>
                </el-button>
              </el-tooltip>

              <el-popconfirm title="确定删除吗？" @confirm="deleteProduct(row)">
                <template #reference>
                  <div style="display: inline-block;">
                    <el-tooltip content="删除" placement="top">
                      <el-button link type="danger" class="icon-btn red">
                        <el-icon :size="18"><Delete /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </template>
    </ProductTable>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '添加商品'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="productForm" label-width="120px" label-position="left">
        <!-- Basic Info -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><InfoFilled /></el-icon>
            <span>基础信息</span>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="商品名称" required>
                <el-input
                  v-model="productForm.name"
                  placeholder="请输入商品名称"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="商品描述">
                <el-input
                  v-model="productForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入商品描述"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="商品图片">
                <ImageUploader v-model="productForm.imageUrl" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="所需积分" required>
                <el-input-number
                  v-model="productForm.price"
                  :min="0"
                  :precision="0"
                  :step="10"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="分类" required>
                <el-select v-model="productForm.category" placeholder="选择分类" style="width: 100%">
                  <el-option label="优惠券" value="coupon" />
                  <el-option label="实物礼品" value="gift" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="库存数量">
                <el-input-number
                  v-model="productForm.stock"
                  :min="0"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- Coupon Configuration -->
        <CouponForm
          v-if="productForm.category === 'coupon'"
          :form="productForm"
          :coffee-products="coffeeProducts"
        />
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" size="large" @click="saveProduct">
            <el-icon class="el-icon--left"><Check /></el-icon>
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Picture, InfoFilled, Check, Edit, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getCoffeeProducts, getPointsProducts,
  addPointsProduct, updatePointsProduct, deletePointsProduct, togglePointsProductStatus
} from '@/api'

import AdminPageHeader from '@/components/ui/AdminPageHeader.vue'
import AdminFilterBar from '@/components/ui/AdminFilterBar.vue'
import ProductTable from './components/ProductTable.vue'
import ImageUploader from './components/ImageUploader.vue'
import CouponForm from './components/CouponForm.vue'
import { getImageUrl } from '@/utils/image'

// State
const loading = ref(false)
const rawData = ref([])
const lastUpdated = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const coffeeProducts = ref([])

const filters = reactive({
  keyword: '',
  category: '',
  status: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

// Category helpers
const getCategoryColor = (cat) => {
  const map = {
    'coupon': '#E91E63',
    'merchandise': '#9C27B0',
    'gift': '#9C27B0'
  }
  return map[cat] || '#607D8B'
}

const getCategoryLabel = (cat) => {
  const map = {
    'coupon': '优惠券',
    'merchandise': '周边商品',
    'gift': '实物礼品'
  }
  return map[cat] || cat
}

const productForm = ref(createEmptyForm())

function createEmptyForm() {
  return {
    name: '', description: '', imageUrl: '',
    price: 0, category: 'merchandise', stock: 0,
    couponType: 'EXCHANGE', couponValue: 85, faceValue: null,
    minOrderAmount: null, linkedProductId: null,
    monthlyLimit: null, validDays: 7
  }
}

// Computed
const filteredData = computed(() => {
  let list = rawData.value
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    list = list.filter(item => item.name && item.name.toLowerCase().includes(kw))
  }
  if (filters.category) {
    list = list.filter(item => item.category === filters.category)
  }
  if (filters.status) {
    if (filters.status === 'sold_out') {
      list = list.filter(item => item.status === 'sold_out' || (item.stock !== undefined && item.stock <= 0))
    } else {
      list = list.filter(item => item.status === filters.status)
    }
  }
  return list
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

// Actions
const loadData = async () => {
  loading.value = true
  try {
    const res = await getPointsProducts()
    rawData.value = res.data || []
    lastUpdated.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const loadCoffeeProducts = async () => {
  try {
    const res = await getCoffeeProducts()
    coffeeProducts.value = res.data || []
  } catch (e) {
    console.warn('加载咖啡商品失败:', e)
  }
}

const handleSearch = () => { currentPage.value = 1 }
const resetFilters = () => {
  filters.keyword = ''
  filters.category = ''
  filters.status = ''
  currentPage.value = 1
}

const showAddModal = () => {
  isEdit.value = false
  editingId.value = null
  productForm.value = {
    ...createEmptyForm(),
    category: 'merchandise'
  }
  dialogVisible.value = true
}

const editProduct = (row) => {
  isEdit.value = true
  editingId.value = row.id
  productForm.value = {
    name: row.name,
    description: row.description,
    imageUrl: row.imageUrl,
    price: row.pointsPrice,
    category: row.category,
    stock: row.stock || 0,
    couponType: row.couponType || 'EXCHANGE',
    couponValue: row.couponValue || 85,
    faceValue: row.faceValue || null,
    minOrderAmount: row.minOrderAmount || null,
    linkedProductId: row.linkedProductId || null,
    monthlyLimit: row.monthlyLimit || null,
    validDays: row.validDays || 7
  }
  dialogVisible.value = true
}

const saveProduct = async () => {
  if (!productForm.value.name) return ElMessage.warning('请输入名称')

  try {
    const data = {
      name: productForm.value.name,
      description: productForm.value.description,
      imageUrl: productForm.value.imageUrl,
      category: productForm.value.category,
      pointsPrice: productForm.value.price,
      stock: productForm.value.stock
    }

    // Coupon configuration
    if (productForm.value.category === 'coupon') {
      const cType = productForm.value.couponType
      data.couponType = cType
      data.productType = 'VIRTUAL'

      if (cType === 'EXCHANGE') {
        data.linkedProductId = productForm.value.linkedProductId
        data.faceValue = productForm.value.faceValue || null
        data.couponValue = null
        data.minOrderAmount = null
      } else if (cType === 'DISCOUNT') {
        data.couponValue = productForm.value.couponValue
        data.faceValue = null
        data.minOrderAmount = null
        data.linkedProductId = null
      } else if (cType === 'FULL_REDUCE') {
        data.couponValue = productForm.value.couponValue
        data.minOrderAmount = productForm.value.minOrderAmount
        data.faceValue = null
        data.linkedProductId = null
      } else if (cType === 'BOGO') {
        data.couponValue = 40
        data.faceValue = null
        data.minOrderAmount = null
        data.linkedProductId = null
      } else if (cType === 'SHOT') {
        data.couponValue = 1
        data.faceValue = null
        data.minOrderAmount = null
        data.linkedProductId = null
      } else if (cType === 'DELIVERY_FEE') {
        data.couponValue = productForm.value.couponValue || 10
        data.faceValue = null
        data.minOrderAmount = null
        data.linkedProductId = null
      }
    }

    // Common settings
    data.monthlyLimit = productForm.value.monthlyLimit || null
    data.validDays = productForm.value.validDays || 7

    if (isEdit.value) await updatePointsProduct(editingId.value, data)
    else await addPointsProduct(data)

    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const toggleStatus = async (row) => {
  try {
    const res = await togglePointsProductStatus(row.id)
    row.status = res.data.status
    const msg = row.status === 'active' ? '上架商品成功' : '下架商品成功'
    ElMessage.success(msg)
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const deleteProduct = async (row) => {
  try {
    await deletePointsProduct(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

onMounted(() => {
  loadData()
  loadCoffeeProducts()
})
</script>

<style scoped lang="scss">
.product-info-cell {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.product-thumb-rounded {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.image-placeholder-rounded {
  width: 100%;
  height: 100%;
  background: #f9f9f9;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #e0e0e0;
  border-radius: 12px;
}

.product-meta-clean {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  overflow: hidden;
  flex: 1;
}

.product-title-bold {
  font-weight: 700;
  font-size: 14px;
  color: #333;
  line-height: 1.2;
}

.product-desc-grey {
  font-size: 12px;
  color: #999;
  line-height: 1.3;
}

.truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-pill {
  font-weight: 600;
  border-radius: 12px;
  padding: 0 12px;
  height: 24px;
  line-height: 24px;
  border: none !important;
}

.action-icons {
  display: flex;
  gap: 12px;
  justify-content: center;
  align-items: center;
}

.icon-btn {
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s;
}

.icon-btn.blue {
  color: #409EFF;
}
.icon-btn.blue:hover {
  background: #ecf5ff;
}

.icon-btn.red {
  color: #F56C6C;
}
.icon-btn.red:hover {
  background: #fef0f0;
}

.points-font {
  color: #B45309;
  font-weight: 600;
}

.form-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background-color: #f5f7fa;
  border-left: 4px solid #722ed1;
  color: #303133;
  border-radius: 4px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 20px;

  .el-icon {
    font-size: 18px;
    color: #722ed1;
    margin-right: 4px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-switch__core) {
  border-color: #e4e7ed;
  background-color: #dcdfe6;
  min-width: 60px;
}
:deep(.el-switch.is-checked .el-switch__core) {
  border-color: #13ce66;
  background-color: #13ce66;
}
</style>
