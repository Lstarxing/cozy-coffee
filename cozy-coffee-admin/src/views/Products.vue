<template>
  <div class="products-page">
    <AdminPageHeader 
      :title="pageTitle" 
      :subtitle="pageSubtitle"
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
      <el-form-item label="分类" v-if="isCoffeeMode">
         <el-select v-model="filters.category" placeholder="全部" clearable style="width: 140px">
            <el-option label="意式咖啡" value="espresso" />
            <el-option label="季节限定" value="signature" />
            <el-option label="精品手冲" value="soe" />
            <el-option label="烘焙甜品" value="bakery" />
            <el-option label="加料/配料" value="addon" />
            <el-option label="其他" value="other" />
         </el-select>
      </el-form-item>
       <el-form-item label="分类" v-else>
         <el-select v-model="filters.category" placeholder="全部" clearable style="width: 140px">
            <el-option label="周边商品" value="merchandise" />
            <el-option label="优惠券" value="coupon" />
         </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="上架" value="active" />
          <el-option label="下架" value="inactive" />
          <el-option v-if="!isCoffeeMode" label="售罄" value="sold_out" />
        </el-select>
      </el-form-item>
    </AdminFilterBar>

    <el-card shadow="never" class="table-card">
      <TableToolbar :last-updated="lastUpdated" @refresh="loadData" />

      <el-table 
        :data="paginatedData" 
        v-loading="loading"
        size="small"
        header-cell-class-name="table-header"
      >
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
                  :content="row.description" 
                  placement="top" 
                  :show-after="500"
                  v-if="row.description"
                >
                  <div class="product-desc-grey truncate">{{ row.description }}</div>
                </el-tooltip>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="isCoffeeMode ? '价格' : '所需积分'" width="150" align="center">
          <template #default="{ row }">
            <template v-if="isCoffeeMode">
              <div class="price-stack">
                <!-- 意式咖啡双规格 -->
                <template v-if="row.category === 'espresso' && row.priceLarge">
                  <div class="price-row">
                    <span class="price-num">¥{{ row.priceMedium || row.price }}</span>
                    <span class="price-label">(中)</span>
                  </div>
                  <div class="price-row">
                    <span class="price-num">¥{{ row.priceLarge }}</span>
                    <span class="price-label">(大)</span>
                  </div>
                </template>
                <!-- 其他商品 -->
                <template v-else>
                  <div class="price-row">
                    <span class="price-num">¥{{ row.priceMedium || row.price }}</span>
                    <span class="price-label">(标)</span>
                  </div>
                </template>
              </div>
            </template>
            <span class="points-font" v-else>{{ row.pointsPrice }} 积分</span>
          </template>
        </el-table-column>

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

        <el-table-column label="库存" width="100" v-if="!isCoffeeMode">
            <template #default="{ row }">
                {{ row.stock }}
            </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'active'"
              active-color="#13ce66"
              inactive-color="#ff4949"
              @change="toggleStatus(row)"
              :loading="row.statusLoading"
            />
          </template>
        </el-table-column>

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
      </el-table>

       <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredData.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- 添加/编辑商品对话框 - 优化版 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑商品' : '添加商品'" 
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="productForm" label-width="120px" label-position="left">
        <!-- 基础信息 -->
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
                <div class="image-upload-section">
                  <el-upload
                    class="avatar-uploader"
                    :show-file-list="false"
                    :before-upload="beforeImageUpload"
                    :http-request="handleImageUpload"
                    accept="image/*"
                  >
                    <img v-if="productForm.imageUrl" :src="getImageUrl(productForm.imageUrl)" class="avatar" />
                    <div v-else class="avatar-uploader-placeholder">
                      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                      <div class="upload-text">点击上传</div>
                    </div>
                  </el-upload>
                  <div class="url-input-wrapper">
                    <el-input 
                      v-model="productForm.imageUrl" 
                      placeholder="或输入图片 URL" 
                      size="small"
                    >
                      <template #prefix>
                        <el-icon><Link /></el-icon>
                      </template>
                    </el-input>
                  </div>
                </div>
              </el-form-item>
            </el-col>
            
            <!-- 咖啡商品价格 v5.0 -->
            <template v-if="isCoffeeMode">
              <!-- 情况 A: 意式咖啡 - 支持中杯/大杯 -->
              <template v-if="productForm.category === 'espresso'">
                <el-col :span="12">
                  <el-form-item label="中杯价格(元)" required>
                    <el-input-number 
                      v-model="productForm.priceMedium" 
                      :min="0" 
                      :precision="2"
                      :step="0.5"
                      controls-position="right"
                      style="width: 100%"
                      placeholder="中杯价格"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="大杯价格(元)">
                    <el-input-number 
                      v-model="productForm.priceLarge" 
                      :min="0" 
                      :precision="2"
                      :step="0.5"
                      controls-position="right"
                      style="width: 100%"
                      placeholder="大杯（选填）"
                    />
                  </el-form-item>
                </el-col>
              </template>

              <!-- 情况 B: 非意式咖啡（甜品、加料、季节限定等）- 仅标准价格 -->
              <template v-else>
                <el-col :span="12">
                  <el-form-item label="标准价格(元)" required>
                    <el-input-number 
                      v-model="productForm.priceMedium" 
                      :min="0" 
                      :precision="2"
                      :step="0.5"
                      controls-position="right"
                      style="width: 100%"
                      placeholder="请输入价格"
                    />
                    <div class="form-hint-inline" v-if="productForm.category === 'addon'">如：额外浓缩 5元</div>
                  </el-form-item>
                </el-col>
              </template>
            </template>
            
            <!-- 积分商品：所需积分 -->
            <el-col :span="12" v-if="!isCoffeeMode">
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
                  <template v-if="isCoffeeMode">
                    <el-option label="☕ 意式咖啡" value="espresso" />
                    <el-option label="⭐ 季节限定" value="signature" />
                    <el-option label="✨ 精品手冲" value="soe" />
                    <el-option label="🍰 烘焙甜品" value="bakery" />
                    <el-option label="➕ 加料/配料" value="addon" />
                    <el-option label="📦 其他" value="other" />
                  </template>
                  <template v-else>
                    <el-option label="优惠券" value="coupon" />
                    <el-option label="实物礼品" value="gift" />
                  </template>
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12" v-if="isCoffeeMode">
              <el-form-item label="新品标识">
                <el-switch v-model="productForm.isNewProduct" active-text="是" inactive-text="否" />
                <div class="form-hint-inline">标记为“New”新品，显示在菜单顶部</div>
              </el-form-item>
            </el-col>
            
            <el-col :span="12" v-if="!isCoffeeMode">
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
        
        <!-- SKU规格配置（仅咖啡商品） v5.2 -->
        <div class="form-section" v-if="isCoffeeMode">
          <div class="section-title">
            <el-icon><Setting /></el-icon>
            <span>SKU规格配置</span>
          </div>
          
          <el-alert 
            type="info" 
            :closable="false"
            show-icon
            style="margin-bottom: 16px"
          >
            <template #title>配置该商品可选的杯型、糖度、温度选项</template>
          </el-alert>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="杯型规格">
                <el-select v-model="productForm.sizeType" placeholder="选择规格" style="width: 100%">
                  <el-option label="仅中杯" value="DEFAULT" />
                  <el-option label="中杯+大杯" value="MEDIUM_LARGE" />
                  <el-option label="全部杯型" value="ALL_SIZES" />
                </el-select>
                <div class="form-hint-inline">DEFAULT=仅中杯</div>
              </el-form-item>
            </el-col>
            
            <el-col :span="8">
              <el-form-item label="糖度选项">
                <el-select v-model="productForm.sugarType" placeholder="选择规格" style="width: 100%">
                  <el-option label="自由选择" value="FREE_CHOICE" />
                  <el-option label="仅无糖" value="NO_SUGAR_ONLY" />
                  <el-option label="最甜少甜" value="MIN_LESS_SWEET" />
                </el-select>
                <div class="form-hint-inline">FREE_CHOICE=全糖度可选</div>
              </el-form-item>
            </el-col>
            
            <el-col :span="8">
              <el-form-item label="温度选项">
                <el-select v-model="productForm.tempType" placeholder="选择规格" style="width: 100%">
                  <el-option label="冷热皆可" value="ALL_OK" />
                  <el-option label="仅冰" value="COLD_ONLY" />
                  <el-option label="仅热" value="HOT_ONLY" />
                  <el-option label="不可热饮" value="NO_HOT" />
                </el-select>
                <div class="form-hint-inline">Dirty类推荐COLD_ONLY</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        
        <!-- 优惠券配置 -->
        <div class="form-section" v-if="!isCoffeeMode && productForm.category === 'coupon'">
          <div class="section-title coupon">
            <el-icon><Ticket /></el-icon>
            <span>优惠券配置</span>
          </div>
          
          <el-alert 
            type="info" 
            :closable="false"
            show-icon
            style="margin-bottom: 16px"
          >
            <template #title>请根据券类型配置相应参数</template>
          </el-alert>
          
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="券类型" required>
                <el-radio-group v-model="productForm.couponType" size="default">
                  <el-radio-button value="EXCHANGE">兑换券</el-radio-button>
                  <el-radio-button value="DISCOUNT">折扣券</el-radio-button>
                  <el-radio-button value="FULL_REDUCE">满减券</el-radio-button>
                  <el-radio-button value="BOGO">买一送一</el-radio-button>
                  <el-radio-button value="SHOT">加浓缩</el-radio-button>
                  <el-radio-button value="DELIVERY_FEE">配送费</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            
            <!-- 兑换券配置 -->
            <template v-if="productForm.couponType === 'EXCHANGE'">
              <el-col :span="24">
                <el-form-item label="可兑换饮品">
                  <el-select 
                    v-model="productForm.linkedProductId" 
                    placeholder="选择可兑换的咖啡商品" 
                    style="width: 100%"
                    filterable
                    clearable
                  >
                    <el-option 
                      :value="null"
                      label="🎫 全场饮品通兑（自动匹配最高价）"
                    />
                    <el-option-group label="指定商品">
                      <el-option 
                        v-for="coffee in coffeeProducts" 
                        :key="coffee.id" 
                        :label="`${coffee.name} (¥${coffee.price})`"
                        :value="coffee.id"
                      />
                    </el-option-group>
                  </el-select>
                  <div class="form-hint-inline" v-if="!productForm.linkedProductId">
                    通兑券将自动抵扣订单中价格最高的饮品（最高40元）
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-alert 
                  type="warning" 
                  :closable="false"
                  show-icon
                >
                  <template #title>注意：使用兑换券下单不会获得积分</template>
                </el-alert>
              </el-col>
            </template>
            
            <!-- 折扣券配置 -->
            <el-col :span="24" v-if="productForm.couponType === 'DISCOUNT'">
              <el-form-item label="折扣率" required>
                <el-slider 
                  v-model="productForm.couponValue" 
                  :min="10" 
                  :max="99"
                  :step="5"
                  show-input
                  :format-tooltip="(val) => `${val / 10}折`"
                />
                <div class="form-hint-block">
                  当前折扣：<strong>{{ (productForm.couponValue / 10).toFixed(1) }}折</strong>
                </div>
              </el-form-item>
            </el-col>
            
            <!-- 满减券配置 -->
            <template v-if="productForm.couponType === 'FULL_REDUCE'">
              <el-col :span="12">
                <el-form-item label="满减门槛(元)">
                  <el-input-number 
                    v-model="productForm.minOrderAmount" 
                    :min="0" 
                    :max="999"
                    controls-position="right"
                    style="width: 100%"
                    placeholder="0表示无门槛"
                  />
                  <div class="form-hint-inline">0 表示无门槛券</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="减免金额(元)" required>
                  <el-input-number 
                    v-model="productForm.couponValue" 
                    :min="1" 
                    :max="999"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <div class="form-hint-block">
                  优惠规则：订单满 <strong>¥{{ productForm.minOrderAmount || 0 }}</strong> 可减 <strong>¥{{ productForm.couponValue || 0 }}</strong>
                </div>
              </el-col>
            </template>
            
            <!-- 买一送一券配置 v5.0 -->
            <template v-if="productForm.couponType === 'BOGO'">
              <el-col :span="24">
                <el-alert 
                  type="success" 
                  :closable="false"
                  show-icon
                  style="margin-bottom: 12px"
                >
                  <template #title>买一送一券：第二杯免费（低价免单，最高抵扣40元）</template>
                </el-alert>
                <div class="bogo-terms">
                  <div class="term-title">核销细则：</div>
                  <ul class="term-list">
                    <li>仅限饮品类：不适用于瓶装饮料、甜品及周边商品</li>
                    <li>低价免单原则：两杯价格不等时，自动核销较低价格</li>
                    <li>不可分次核销：单笔订单需包含至少2杯饮品</li>
                  </ul>
                </div>
              </el-col>
            </template>
            
            <!-- 加浓缩券配置 v5.0 -->
            <template v-if="productForm.couponType === 'SHOT'">
              <el-col :span="24">
                <el-alert 
                  type="info" 
                  :closable="false"
                  show-icon
                >
                  <template #title>加浓缩券（附加券）：下单时可额外添加1份浓缩，可与主券叠加使用</template>
                </el-alert>
              </el-col>
            </template>
            
            <!-- 配送费抵扣券配置 v5.0 -->
            <template v-if="productForm.couponType === 'DELIVERY_FEE'">
              <el-col :span="12">
                <el-form-item label="最高抵扣(元)">
                  <el-input-number 
                    v-model="productForm.couponValue" 
                    :min="1" 
                    :max="99"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-alert 
                  type="info" 
                  :closable="false"
                  show-icon
                >
                  <template #title>配送费抵扣券（附加券）：仅抵扣外卖订单配送费，可与主券叠加使用</template>
                </el-alert>
              </el-col>
            </template>
          </el-row>
        </div>
        
        <!-- 通用配置 -->
        <div class="form-section" v-if="!isCoffeeMode">
          <div class="section-title settings">
            <el-icon><Setting /></el-icon>
            <span>通用配置</span>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="月度限购">
                <el-input-number 
                  v-model="productForm.monthlyLimit" 
                  :min="0" 
                  :max="999"
                  placeholder="0为不限制"
                  controls-position="right"
                  style="width: 100%"
                />
                <div class="form-hint-inline">
                  0 或空表示不限制
                </div>
              </el-form-item>
            </el-col>
            
            <el-col :span="12" v-if="productForm.category === 'coupon'">
              <el-form-item label="券有效期(天)">
                <el-input-number 
                  v-model="productForm.validDays" 
                  :min="1" 
                  :max="365"
                  controls-position="right"
                  style="width: 100%"
                />
                <div class="form-hint-inline">
                  兑换后券的有效天数
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="saveProduct" size="large">
            <el-icon class="el-icon--left"><Check /></el-icon>
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

import { Plus, Picture, InfoFilled, Ticket, Setting, Link, Check, Edit, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { 
  getCoffeeProducts, getPointsProducts, 
  addCoffeeProduct, updateCoffeeProduct, deleteCoffeeProduct, toggleCoffeeProductStatus,
  addPointsProduct, updatePointsProduct, deletePointsProduct, togglePointsProductStatus,
  uploadImage
} from '../api'

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import AdminFilterBar from '../components/ui/AdminFilterBar.vue'
import TableToolbar from '../components/ui/TableToolbar.vue'
import StatusTag from '../components/ui/StatusTag.vue'

const route = useRoute()

// State
const loading = ref(false)
const rawData = ref([])
const lastUpdated = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const coffeeProducts = ref([]) // 咖啡商品列表（用于兑换券选择）

const filters = reactive({
    keyword: '',
    category: '',
    status: ''
})

const currentPage = ref(1)
const pageSize = ref(10)



// Helper functions for UI
const getCategoryColor = (cat) => {
    const map = {
        'espresso': '#795548',   // Brown
        'signature': '#FFC107',  // Gold
        'soe': '#6D4C41',        // Deep Brown (精品手冲)
        'bakery': '#FF9800',     // Orange
        'addon': '#9E9E9E',      // Grey
        'coupon': '#E91E63',     // Pink
        'merchandise': '#9C27B0' // Purple
    }
    return map[cat] || '#607D8B'
}

const getCategoryLabel = (cat) => {
    const map = {
        'espresso': '意式咖啡',
        'signature': '季节限定',
        'soe': '精品手冲',
        'bakery': '烘焙甜品',
        'addon': '加料/配料',
        'other': '其他',
        'coupon': '优惠券',
        'merchandise': '周边商品',
        'gift': '实物礼品'
    }
    return map[cat] || cat
}

const productForm = ref({
  name: '',
  description: '',
  imageUrl: '',
  price: 0,
  priceMedium: null,   // v5.0: 中杯价格
  priceLarge: null,    // v5.0: 大杯价格
  category: '',
  stock: 0,
  // 优惠券配置字段
  couponType: 'EXCHANGE',
  couponValue: 85,       // 折扣率或满减金额
  faceValue: null,       // 兑换券抵扣面值
  minOrderAmount: null,  // 满减门槛
  linkedProductId: null, // 兑换券关联的咖啡商品ID
  // v4.2 通用配置
  monthlyLimit: null,    // 月度限购
  validDays: 7           // 券有效期（默认7天）
})

// Computed
const isCoffeeMode = computed(() => route.path.includes('/coffee'))
const pageTitle = computed(() => isCoffeeMode.value ? '咖啡菜单管理' : '积分商品管理')
const pageSubtitle = computed(() => isCoffeeMode.value ? '管理咖啡与甜品菜单' : '管理积分兑换权益商品')

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
            // 售罄：status='sold_out' 或 stock<=0
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

// Watchers
watch(() => route.path, () => {
    loadData()
    resetFilters()
})

// Actions
const loadData = async () => {
    loading.value = true
    try {
        const apiFunc = isCoffeeMode.value ? getCoffeeProducts : getPointsProducts
        const res = await apiFunc()
        rawData.value = res.data || []
        lastUpdated.value = dayjs().format('HH:mm:ss')
    } catch (e) {
        ElMessage.error('加载失败: ' + e.message)
    } finally {
        loading.value = false
    }
}

// 加载咖啡商品列表（用于兑换券选择）
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

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const showAddModal = () => {
    isEdit.value = false
    editingId.value = null
    productForm.value = {
        name: '', description: '', imageUrl: '', 
        price: 0, priceMedium: null, priceLarge: null,
        category: isCoffeeMode.value ? 'espresso' : 'merchandise', stock: 0,
        isNewProduct: false, // v5.0
        // v5.2 SKU配置（咖啡商品）
        sizeType: 'MEDIUM_LARGE',   // 默认：中/大杯
        sugarType: 'FREE_CHOICE',   // 默认：自由选择
        tempType: 'ALL_OK',         // 默认：冷热皆可
        // 优惠券默认配置
        couponType: 'EXCHANGE', couponValue: 85, faceValue: null,
        minOrderAmount: null, linkedProductId: null,
        // v4.2 通用配置
        monthlyLimit: null, validDays: 7
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
        price: isCoffeeMode.value ? row.price : row.pointsPrice,
        priceMedium: row.priceMedium || null,  // v5.0: 中杯价格
        priceLarge: row.priceLarge || null,    // v5.0: 大杯价格
        category: row.category,
        stock: row.stock || 0,
        isNewProduct: row.isNewProduct || false, // v5.0
        // v5.2 SKU配置（咖啡商品）
        sizeType: row.sizeType || 'MEDIUM_LARGE',
        sugarType: row.sugarType || 'FREE_CHOICE',
        tempType: row.tempType || 'ALL_OK',
        // 优惠券配置
        couponType: row.couponType || 'EXCHANGE',
        couponValue: row.couponValue || 85,
        faceValue: row.faceValue || null,
        minOrderAmount: row.minOrderAmount || null,
        linkedProductId: row.linkedProductId || null,
        // v4.2 通用配置
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
            category: productForm.value.category
        }
        
        // Mode specifics
        if (isCoffeeMode.value) {
            // v5.0: 中杯价格作为默认价格
            data.price = productForm.value.priceMedium || productForm.value.price || 0
            data.priceMedium = productForm.value.priceMedium || null
            data.priceLarge = productForm.value.priceLarge || null
            data.isNewProduct = productForm.value.isNewProduct || false // v5.0
            // v5.2 SKU配置
            data.sizeType = productForm.value.sizeType || 'MEDIUM_LARGE'
            data.sugarType = productForm.value.sugarType || 'FREE_CHOICE'
            data.tempType = productForm.value.tempType || 'ALL_OK'
        } else {
            data.pointsPrice = productForm.value.price
            data.stock = productForm.value.stock
            
            // 优惠券配置（仅当分类为优惠券时）
            // 优惠券配置（仅当分类为优惠券时）
            if (productForm.value.category === 'coupon') {
                const cType = productForm.value.couponType
                data.couponType = cType
                data.productType = 'VIRTUAL'
                
                // 根据类型设置字段，无关字段设为 null
                if (cType === 'EXCHANGE') {
                    data.linkedProductId = productForm.value.linkedProductId
                    // 仅当用户填写了faceValue时才传，否则传null(由后端处理或默认为商品原价)
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
                    // v5.0 买一送一券：使用 couponValue 存储最高抵扣金额
                    data.couponValue = 40 // 默认最高抵扣40元
                    data.faceValue = null
                    data.minOrderAmount = null
                    data.linkedProductId = null
                } else if (cType === 'SHOT') {
                    // v5.0 加浓缩券（附加券）
                    data.couponValue = 1 // 表示1份浓缩
                    data.faceValue = null
                    data.minOrderAmount = null
                    data.linkedProductId = null
                } else if (cType === 'DELIVERY_FEE') {
                    // v5.0 配送费抵扣券（附加券）
                    data.couponValue = productForm.value.couponValue || 10 // 最高抵扣金额
                    data.faceValue = null
                    data.minOrderAmount = null
                    data.linkedProductId = null
                }
            }
            
            // v4.2 通用配置（所有积分商品）
            data.monthlyLimit = productForm.value.monthlyLimit || null
            data.validDays = productForm.value.validDays || 7
        }

        if (isCoffeeMode.value) {
            if (isEdit.value) await updateCoffeeProduct(editingId.value, data)
            else await addCoffeeProduct(data)
        } else {
            if (isEdit.value) await updatePointsProduct(editingId.value, data)
            else await addPointsProduct(data)
        }
        
        ElMessage.success('保存成功')
        dialogVisible.value = false
        loadData()
    } catch (e) {
        ElMessage.error('保存失败: ' + e.message)
    }
}

const toggleStatus = async (row) => {
    try {
        const apiFunc = isCoffeeMode.value ? toggleCoffeeProductStatus : togglePointsProductStatus
        const res = await apiFunc(row.id)
        // Optimistic update or use response
        row.status = res.data.status
        const msg = row.status === 'active' ? '上架商品成功' : '下架商品成功'
        ElMessage.success(msg)
    } catch (e) {
        ElMessage.error('操作失败: ' + e.message)
    }
}

const deleteProduct = async (row) => {
    try {
        const apiFunc = isCoffeeMode.value ? deleteCoffeeProduct : deletePointsProduct
        await apiFunc(row.id)
        ElMessage.success('删除成功')
        loadData()
    } catch (e) {
        ElMessage.error('删除失败: ' + e.message)
    }
}

// Image Upload
const beforeImageUpload = (file) => {
    if (file.size / 1024 / 1024 > 5) {
        ElMessage.error('图片不能超过 5MB')
        return false
    }
    return true
}

const handleImageUpload = async (opt) => {
    try {
        const res = await uploadImage(opt.file)
        if (res.data?.url) {
            productForm.value.imageUrl = res.data.url
            ElMessage.success('上传成功')
        } else {
            ElMessage.error('上传成功但未返回图片 URL')
        }
    } catch (e) {
        console.error('图片上传失败:', e)
        ElMessage.error('上传失败: ' + (e.message || '未知错误'))
    }
}

onMounted(() => {
    loadData()
    // 加载咖啡商品列表（用于兑换券选择）
    if (!route.path.includes('/coffee')) {
        loadCoffeeProducts()
    }
})
</script>

<style scoped lang="scss">
.table-card {
  border: 1px solid #E5E7EB;
  border-radius: 6px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .product-thumb {
    width: 48px;
    height: 48px;
    border-radius: 6px;
    background: #F3F4F6;
    border: 1px solid #E5E7EB;
    flex-shrink: 0;
  }
  
  .product-meta {
    overflow: hidden;
    
    .product-name {
      font-weight: 500;
      color: #111827;
      margin-bottom: 2px;
    }
    
    .product-desc {
      font-size: 12px;
    }
  }
}

.price-font {
    font-family: monospace;
    font-weight: 600;
}

.points-font {
    color: #B45309;
    font-weight: 600;
}

.truncate {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.image-upload-wrapper {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    
    .url-input {
        flex: 1;
    }
}

.avatar-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 100px;
    height: 100px;
    display: flex;
    justify-content: center;
    align-items: center;
    
    &:hover {
        border-color: var(--el-color-primary);
    }
}
.avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
}
.avatar {
    width: 100px;
    height: 100px;
    display: block;
    object-fit: cover;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.form-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.form-hint-inline {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.form-hint-block {
  padding: 8px 12px;
  background: #F5F7FA;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
  margin-top: 8px;
  
  strong {
    color: var(--el-color-primary);
    font-weight: 600;
  }
}

// 优化后的弹窗样式
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
  background-color: #f5f7fa; /* 默认浅灰背景 */
  border-left: 4px solid #722ed1; /* 紫色装饰线 */
  color: #303133;
  border-radius: 4px; /* 更小的圆角 */
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 20px;
  /* 移除阴影，更加扁平 */
  
  .el-icon {
    font-size: 18px;
    color: #722ed1; /* 图标跟随装饰线颜色 */
    margin-right: 4px;
  }
  
  &.coupon {
    background-color: #fff0f6; /* 极淡粉红背景 */
    border-left-color: #eb2f96;
    color: #303133;
    
    .el-icon { color: #eb2f96; }
  }
  
  &.settings {
    background-color: #e6f7ff; /* 极淡蓝背景 */
    border-left-color: #1890ff;
    color: #303133;
    
    .el-icon { color: #1890ff; }
  }
}

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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* v5.0 买一送一券核销细则样式 */
.bogo-terms {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  
  .term-title {
    font-weight: 600;
    color: #374151;
    margin-bottom: 8px;
  }
  
  .term-list {
    margin: 0;
    padding-left: 20px;
    color: #6b7280;
    font-size: 13px;
    
    li {
      margin-bottom: 4px;
      &:last-child { margin-bottom: 0; }
    }
  }
}

/* v5.0 价格显示 */
.price-display {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.price-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.size-label {
  font-size: 11px;
  color: #9ca3af;
  padding: 1px 4px;
  background: #f3f4f6;
  border-radius: 3px;
}

.price-font {
  font-weight: 600;
  color: #d97706;
}

.points-font {
  font-weight: 600;
  color: #6366f1;
}

</style>

<style scoped>
/* New UI Styles v5.0 Refactor */
.product-info-cell {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.product-thumb-rounded {
  width: 52px;
  height: 52px;
  border-radius: 6px; /* Refined 6px radius */
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
   /* Remove default border */
   border: none !important;
}

/* Action Icons */
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

/* Price Column Optimization */
.price-stack {
  display: flex;
  flex-direction: column;
  align-items: center; /* Center align in the cell */
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
  transform: scale(0.9); /* Slightly smaller visual */
}

/* Status Switch Override */
:deep(.el-switch__core) {
    border-color: #e4e7ed;
    background-color: #dcdfe6;
    min-width: 60px; /* Ensure space for text */
}
:deep(.el-switch.is-checked .el-switch__core) {
    border-color: #13ce66;
    background-color: #13ce66;
}
</style>
