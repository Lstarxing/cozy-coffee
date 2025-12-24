<template>
  <div class="products-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品管理</span>
          <el-button type="primary" @click="showAddModal">
            <el-icon><Plus /></el-icon> 添加商品
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="咖啡商品" name="coffee">
          <el-table :data="paginatedCoffee" v-loading="coffeeLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="图片" width="100">
              <template #default="{ row }">
                <el-image 
                  :src="getImageUrl(row.imageUrl)" 
                  style="width: 60px; height: 60px; border-radius: 8px;" 
                  fit="cover"
                  :preview-src-list="[getImageUrl(row.imageUrl)]"
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="price" label="价格">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="category" label="分类" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-switch 
                  :model-value="row.status === 'active'" 
                  @change="toggleStatus(row, 'coffee')"
                  active-text="上架" 
                  inactive-text="下架"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text type="primary" @click="editProduct(row, 'coffee')">编辑</el-button>
                <el-button text type="danger" @click="deleteProduct(row, 'coffee')">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="coffeePage"
            :page-size="pageSize"
            :total="coffeeProducts.length"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>

        <el-tab-pane label="积分商品" name="points">
          <el-table :data="paginatedPoints" v-loading="pointsLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="图片" width="100">
              <template #default="{ row }">
                <el-image 
                  :src="getImageUrl(row.imageUrl)" 
                  style="width: 60px; height: 60px; border-radius: 8px;" 
                  fit="cover"
                  :preview-src-list="[getImageUrl(row.imageUrl)]"
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Present /></el-icon>
                    </div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="pointsPrice" label="所需积分">
              <template #default="{ row }">{{ row.pointsPrice }} 积分</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" />
            <el-table-column prop="category" label="类型" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-switch 
                  :model-value="row.status === 'active'" 
                  @change="toggleStatus(row, 'points')"
                  active-text="上架" 
                  inactive-text="下架"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text type="primary" @click="editProduct(row, 'points')">编辑</el-button>
                <el-button text type="danger" @click="deleteProduct(row, 'points')">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pointsPage"
            :page-size="pageSize"
            :total="pointsProducts.length"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: flex-end;"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '添加商品'" width="500px">
      <el-form :model="productForm" label-width="100px">
        <el-form-item label="商品名称">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="productForm.description" type="textarea" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="productForm.imageUrl" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item :label="activeTab === 'coffee' ? '价格(元)' : '所需积分'">
          <el-input-number v-model="productForm.price" :min="0" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="productForm.category" placeholder="选择分类">
            <template v-if="activeTab === 'coffee'">
              <el-option label="咖啡" value="coffee" />
              <el-option label="甜品" value="dessert" />
              <el-option label="其他" value="other" />
            </template>
            <template v-else>
              <el-option label="饮品券" value="drink" />
              <el-option label="周边商品" value="merchandise" />
              <el-option label="优惠券" value="coupon" />
            </template>
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeTab === 'points'" label="库存">
          <el-input-number v-model="productForm.stock" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getCoffeeProducts, getPointsProducts, 
  addCoffeeProduct, updateCoffeeProduct, deleteCoffeeProduct, toggleCoffeeProductStatus 
} from '../api'

const coffeeLoading = ref(false)
const pointsLoading = ref(false)
const activeTab = ref('coffee')
const pageSize = ref(10)
const coffeePage = ref(1)
const pointsPage = ref(1)

const coffeeProducts = ref([])
const pointsProducts = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingProduct = ref(null)
const productForm = ref({
  name: '',
  description: '',
  imageUrl: '',
  price: 0,
  category: '',
  stock: 0
})

// 分页数据
const paginatedCoffee = computed(() => {
  const start = (coffeePage.value - 1) * pageSize.value
  return coffeeProducts.value.slice(start, start + pageSize.value)
})

const paginatedPoints = computed(() => {
  const start = (pointsPage.value - 1) * pageSize.value
  return pointsProducts.value.slice(start, start + pageSize.value)
})

// 获取图片URL（处理相对路径）
const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  // 如果是相对路径，加上后端基础URL
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const loadCoffeeProducts = async () => {
  coffeeLoading.value = true
  try {
    const res = await getCoffeeProducts()
    coffeeProducts.value = res.data || []
  } catch (e) {
    ElMessage.error('加载咖啡商品失败: ' + e.message)
  } finally {
    coffeeLoading.value = false
  }
}

const loadPointsProducts = async () => {
  pointsLoading.value = true
  try {
    const res = await getPointsProducts()
    pointsProducts.value = res.data || []
  } catch (e) {
    ElMessage.error('加载积分商品失败: ' + e.message)
  } finally {
    pointsLoading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'coffee' && coffeeProducts.value.length === 0) {
    loadCoffeeProducts()
  } else if (tab === 'points' && pointsProducts.value.length === 0) {
    loadPointsProducts()
  }
}

const showAddModal = () => {
  isEdit.value = false
  editingProduct.value = null
  productForm.value = {
    name: '',
    description: '',
    imageUrl: '',
    price: 0,
    category: activeTab.value === 'coffee' ? 'coffee' : 'drink',
    stock: 0
  }
  dialogVisible.value = true
}

const editProduct = (row, type) => {
  isEdit.value = true
  editingProduct.value = row
  productForm.value = {
    name: row.name,
    description: row.description || '',
    imageUrl: row.imageUrl || '',
    price: type === 'coffee' ? row.price : row.pointsPrice,
    category: row.category || '',
    stock: row.stock || 0
  }
  dialogVisible.value = true
}

const saveProduct = async () => {
  if (!productForm.value.name) {
    ElMessage.warning('请输入商品名称')
    return
  }
  
  try {
    if (activeTab.value === 'coffee') {
      const productData = {
        name: productForm.value.name,
        description: productForm.value.description,
        imageUrl: productForm.value.imageUrl,
        price: productForm.value.price,
        category: productForm.value.category
      }
      
      if (isEdit.value && editingProduct.value) {
        await updateCoffeeProduct(editingProduct.value.id, productData)
        ElMessage.success('商品已更新')
      } else {
        await addCoffeeProduct(productData)
        ElMessage.success('商品已添加')
      }
      await loadCoffeeProducts()
    } else {
      // 积分商品暂不支持后端CRUD
      ElMessage.info('积分商品管理功能开发中')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

const toggleStatus = async (row, type) => {
  try {
    if (type === 'coffee') {
      const res = await toggleCoffeeProductStatus(row.id)
      row.status = res.data.status
      ElMessage.success(`商品已${row.status === 'active' ? '上架' : '下架'}`)
    } else {
      const newStatus = row.status === 'active' ? 'inactive' : 'active'
      row.status = newStatus
      ElMessage.info('积分商品状态切换功能开发中')
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const deleteProduct = async (row, type) => {
  await ElMessageBox.confirm('确定删除该商品吗？此操作不可撤销', '警告', { type: 'warning' })
  try {
    if (type === 'coffee') {
      await deleteCoffeeProduct(row.id)
      ElMessage.success('商品已删除')
      await loadCoffeeProducts()
    } else {
      ElMessage.info('积分商品删除功能开发中')
    }
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  }
}

onMounted(() => {
  loadCoffeeProducts()
  loadPointsProducts()
})
</script>

<style scoped>
.products-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-placeholder {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  color: #909399;
  font-size: 24px;
}
</style>
