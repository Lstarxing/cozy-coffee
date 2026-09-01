<template>
  <div class="products-page">
    <AdminPageHeader
      title="咖啡菜单管理"
      subtitle="管理咖啡与甜品菜单"
    >
      <template #actions>
        <el-button @click="beanListVisible = true">
          <el-icon class="el-icon--left"><CoffeeCup /></el-icon>
          今日豆单
        </el-button>
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
          <el-option label="01 经典咖啡" value="ESPRESSO" />
          <el-option label="02 奶咖" value="MILK" />
          <el-option label="03 招牌特调" value="SIGNATURE" />
          <el-option label="04 精品咖啡" value="SPECIALTY" />
          <el-option label="05 非咖啡" value="NON_COFFEE" />
          <el-option label="06 烘焙轻食" value="BAKERY" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="上架" value="active" />
          <el-option label="下架" value="inactive" />
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
                <div v-if="row.tags && row.tags.length" class="product-tags">
                  <span v-for="t in row.tags" :key="t" class="product-tag" :style="{ color: tagColor(t), background: tagBg(t) }">
                    {{ tagLabel(t) }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
      </template>

      <template #price>
        <el-table-column label="价格" width="150" align="center">
          <template #default="{ row }">
            <div class="price-stack">
              <template v-if="row.sizeType === 'MEDIUM_LARGE' && row.priceLarge">
                <div class="price-row">
                  <span class="price-num">¥{{ row.priceMedium || row.price }}</span>
                  <span class="price-label">(中)</span>
                </div>
                <div class="price-row">
                  <span class="price-num">¥{{ row.priceLarge }}</span>
                  <span class="price-label">(大)</span>
                </div>
              </template>
              <template v-else>
                <div class="price-row">
                  <span class="price-num">¥{{ row.priceMedium || row.price }}</span>
                  <span class="price-label">(标)</span>
                </div>
              </template>
            </div>
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

      <template #stock />

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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-icons">
              <el-tooltip content="加料组" placement="top">
                <el-button link type="primary" class="icon-btn green" @click="openAddonEditor(row)">
                  <el-icon :size="18"><Setting /></el-icon>
                </el-button>
              </el-tooltip>

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
        <CoffeeProductForm :form="productForm" :beans="beans" :blends="blends" />
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

    <AddonGroupEditorDialog
      v-model:visible="addonDialogVisible"
      :product="addonEditingProduct"
      @saved="loadData"
    />

    <!-- 今日豆单：勾选今日上架的精品 Bean（菜单 04 实时更新） -->
    <el-dialog v-model="beanListVisible" title="今日豆单" width="520px" :close-on-click-modal="false">
      <div class="bean-list-hint">勾选今日上架的手冲 Bean（菜单 04 精品咖啡实时更新）；下架 = 从菜单移除</div>
      <div v-for="bean in todayBeans" :key="bean.id" class="bean-list-item">
        <span class="bean-name">{{ bean.name }}</span>
        <span class="bean-price">手冲 ¥{{ bean.price }}{{ bean.coldBrewPrice ? ` · 冷萃 ¥${bean.coldBrewPrice}` : '' }}</span>
        <el-switch :model-value="bean.status === 'active'" @change="(v) => toggleTodayBean(bean, v)" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Picture, Check, Edit, Delete, Setting, CoffeeCup } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getCoffeeProducts, addCoffeeProduct, updateCoffeeProduct,
  deleteCoffeeProduct, toggleCoffeeProductStatus,
  getBeans, getBlends
} from '@/api'

import AdminPageHeader from '@/components/ui/AdminPageHeader.vue'
import AdminFilterBar from '@/components/ui/AdminFilterBar.vue'
import ProductTable from './components/ProductTable.vue'
import CoffeeProductForm from './components/CoffeeProductForm.vue'
import AddonGroupEditorDialog from './components/AddonGroupEditorDialog.vue'
import { getImageUrl } from '@/utils/image'
import { PRODUCT_CATEGORY_MAP, PRODUCT_TAG_MAP } from '@/constants/product'

// State
const loading = ref(false)
const rawData = ref([])
const lastUpdated = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const addonDialogVisible = ref(false)
const addonEditingProduct = ref(null)
const beans = ref([])
const blends = ref([])
const beanListVisible = ref(false)

// 今日豆单：04 精品咖啡的 Bean 商品（brewMethod 非空），按 sort_order 排序
const todayBeans = computed(() => rawData.value
  .filter(p => p.category === 'SPECIALTY' && p.brewMethod)
  .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)))

const toggleTodayBean = async (bean, active) => {
  try {
    const res = await toggleCoffeeProductStatus(bean.id)
    bean.status = res.data.status
    ElMessage.success(active ? `「${bean.name}」今日上架` : `「${bean.name}」已从今日豆单移除`)
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const filters = reactive({
  keyword: '',
  category: '',
  status: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

// Category helpers
const getCategoryColor = (cat) => {
  return PRODUCT_CATEGORY_MAP[cat]?.color || '#607D8B'
}

const getCategoryLabel = (cat) => {
  return PRODUCT_CATEGORY_MAP[cat]?.label || cat
}

// Tag helpers
const tagLabel = (t) => PRODUCT_TAG_MAP[t]?.label || t
const tagColor = (t) => PRODUCT_TAG_MAP[t]?.color || '#8B5E3C'
const tagBg = (t) => `${tagColor(t)}18`

const productForm = ref(createEmptyForm())

function createEmptyForm() {
  return {
    name: '', description: '', shortDescription: '', imageUrl: '', productCode: '',
    price: null, priceMedium: null, priceLarge: null,
    category: 'ESPRESSO',
    isNewProduct: false,
    sizeType: 'MEDIUM_LARGE',
    sugarType: 'FREE_CHOICE',
    tempType: 'HOT_COLD',
    defaultSugarLevel: 'STANDARD',
    beanId: null,
    blendId: null,
    tags: [],
    brewMethod: '',
    coldBrewPrice: null
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
    list = list.filter(item => item.status === filters.status)
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
    const res = await getCoffeeProducts()
    rawData.value = res.data || []
    lastUpdated.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
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
  productForm.value = createEmptyForm()
  dialogVisible.value = true
}

const editProduct = (row) => {
  isEdit.value = true
  editingId.value = row.id
  productForm.value = {
    name: row.name,
    productCode: row.productCode || '',
    description: row.description,
    shortDescription: row.shortDescription || '',
    imageUrl: row.imageUrl,
    price: row.price,
    priceMedium: row.priceMedium || null,
    priceLarge: row.priceLarge || null,
    category: row.category,
    isNewProduct: row.isNewProduct || false,
    sizeType: row.sizeType || 'MEDIUM_LARGE',
    sugarType: row.sugarType || 'FREE_CHOICE',
    tempType: row.tempType || 'HOT_COLD',
    defaultSugarLevel: row.defaultSugarLevel || (row.sugarType && row.sugarType !== 'NO_SUGAR_ONLY' ? 'STANDARD' : null),
    beanId: row.beanId || null,
    blendId: row.blendId || null,
    tags: row.tags || [],
    brewMethod: row.brewMethod || '',
    coldBrewPrice: row.coldBrewPrice || null
  }
  dialogVisible.value = true
}

const saveProduct = async () => {
  if (!productForm.value.name) return ElMessage.warning('请输入名称')

  try {
    // V2 价格互斥：MEDIUM_LARGE → medium+large（price NULL）；DEFAULT → price
    const isMediumLarge = productForm.value.sizeType === 'MEDIUM_LARGE'
    const data = {
      name: productForm.value.name,
      productCode: productForm.value.productCode || null,
      description: productForm.value.description,
      shortDescription: productForm.value.shortDescription || null,
      imageUrl: productForm.value.imageUrl,
      category: productForm.value.category,
      price: isMediumLarge ? null : (productForm.value.price ?? 0),
      priceMedium: isMediumLarge ? (productForm.value.priceMedium || null) : null,
      priceLarge: isMediumLarge ? (productForm.value.priceLarge || null) : null,
      isNewProduct: productForm.value.isNewProduct || false,
      sizeType: productForm.value.sizeType || 'MEDIUM_LARGE',
      sugarType: productForm.value.sugarType || 'FREE_CHOICE',
      tempType: productForm.value.tempType || 'HOT_COLD',
      defaultSugarLevel: productForm.value.defaultSugarLevel || null,
      beanId: productForm.value.beanId || null,
      blendId: productForm.value.blendId || null,
      tags: productForm.value.tags || [],
      brewMethod: productForm.value.brewMethod || null,
      coldBrewPrice: productForm.value.coldBrewPrice || null
    }

    if (isEdit.value) await updateCoffeeProduct(editingId.value, data)
    else await addCoffeeProduct(data)

    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const openAddonEditor = (row) => {
  addonEditingProduct.value = row
  addonDialogVisible.value = true
}

const toggleStatus = async (row) => {
  try {
    const res = await toggleCoffeeProductStatus(row.id)
    row.status = res.data.status
    const msg = row.status === 'active' ? '上架商品成功' : '下架商品成功'
    ElMessage.success(msg)
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const deleteProduct = async (row) => {
  try {
    await deleteCoffeeProduct(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

const loadBeansAndBlends = async () => {
  try {
    const [beanRes, blendRes] = await Promise.all([getBeans(), getBlends()])
    beans.value = beanRes.data || []
    blends.value = blendRes.data || []
  } catch (e) {
    console.warn('加载豆/拼配列表失败', e)
  }
}

onMounted(() => {
  loadData()
  loadBeansAndBlends()
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

.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.product-tag {
  font-size: 11px;
  line-height: 1;
  padding: 3px 7px;
  border-radius: 4px;
}

.bean-list-hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.bean-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid #F0F2F5;
}

.bean-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.bean-price {
  font-size: 12px;
  color: #8B5E3C;
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

.icon-btn.green {
  color: #67C23A;
}
.icon-btn.green:hover {
  background: #f0f9eb;
}

.icon-btn.red {
  color: #F56C6C;
}
.icon-btn.red:hover {
  background: #fef0f0;
}

.price-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  line-height: 1.2;
}

.price-num {
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 15px;
  color: #2c3e50;
}

.price-label {
  font-size: 12px;
  color: #909399;
  transform: scale(0.9);
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
