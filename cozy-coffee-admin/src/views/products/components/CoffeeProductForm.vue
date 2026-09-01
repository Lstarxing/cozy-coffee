<template>
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
            v-model="form.name"
            placeholder="请输入商品名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-col>

      <el-col :span="24">
        <el-form-item label="商品短码">
          <el-input v-model="form.productCode" placeholder="如 02-caffe-latte" maxlength="64" />
          <div class="form-hint-inline">商品身份标识（3.5 资产命名），用于迁移/图片定位；勿随意改动</div>
        </el-form-item>
      </el-col>

      <el-col :span="24">
        <el-form-item label="列表描述">
          <el-input
            v-model="form.shortDescription"
            type="textarea"
            :rows="1"
            maxlength="40"
            show-word-limit
            placeholder="菜单列表凝练一句（≤40 字，完整展示不省略）"
          />
        </el-form-item>
      </el-col>

      <el-col :span="24">
        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="选规格/详情页展示的完整描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-col>

      <el-col :span="24">
        <el-form-item label="商品图片">
          <ImageUploader v-model="form.imageUrl" />
        </el-form-item>
      </el-col>

      <!-- MEDIUM_LARGE: medium + large -->
      <template v-if="form.sizeType === 'MEDIUM_LARGE'">
        <el-col :span="12">
          <el-form-item label="中杯价格(元)" required>
            <el-input-number
              v-model="form.priceMedium"
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
          <el-form-item label="大杯价格(元)" required>
            <el-input-number
              v-model="form.priceLarge"
              :min="0"
              :precision="2"
              :step="0.5"
              controls-position="right"
              style="width: 100%"
              placeholder="大杯价格"
            />
          </el-form-item>
        </el-col>
      </template>

      <!-- DEFAULT: standard price only -->
      <template v-else>
        <el-col :span="12">
          <el-form-item label="标准价格(元)" required>
            <el-input-number
              v-model="form.price"
              :min="0"
              :precision="2"
              :step="0.5"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入价格"
            />
          </el-form-item>
        </el-col>
      </template>

      <el-col :span="12">
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option label="01 经典咖啡" value="ESPRESSO" />
            <el-option label="02 奶咖" value="MILK" />
            <el-option label="03 招牌特调" value="SIGNATURE" />
            <el-option label="04 精品咖啡" value="SPECIALTY" />
            <el-option label="05 非咖啡" value="NON_COFFEE" />
            <el-option label="06 烘焙轻食" value="BAKERY" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item label="新品标识">
          <el-switch v-model="form.isNewProduct" active-text="是" inactive-text="否" />
          <div class="form-hint-inline">标记为"New"新品，显示在菜单顶部</div>
        </el-form-item>
      </el-col>

      <el-col :span="24">
        <el-form-item label="商品标签">
          <el-select v-model="form.tags" multiple placeholder="选择标签" style="width: 100%">
            <el-option v-for="t in tagOptions" :key="t.value" :value="t.value" :label="t.label" />
          </el-select>
          <div class="form-hint-inline">NEW/COLD/FRUITY 等展示标签；TOP1 由真实销售数据驱动，不手动录入</div>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item label="出品方式">
          <el-select v-model="form.brewMethod" style="width: 100%">
            <el-option label="无（非 Bean 商品）" value="" />
            <el-option label="精品 Bean（手冲/冷萃可选）" value="POUR_OVER" />
          </el-select>
          <div class="form-hint-inline">精品 Bean 商品用户可选手冲或冷萃</div>
        </el-form-item>
      </el-col>
      <el-col v-if="form.brewMethod" :span="12">
        <el-form-item label="冷萃价(元)">
          <el-input-number v-model="form.coldBrewPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          <div class="form-hint-inline">冷萃出品价；手冲用基础价</div>
        </el-form-item>
      </el-col>
    </el-row>
  </div>

  <!-- SKU Configuration -->
  <div class="form-section">
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
          <el-select v-model="form.sizeType" placeholder="选择规格" style="width: 100%">
            <el-option label="仅中杯" value="DEFAULT" />
            <el-option label="中杯+大杯" value="MEDIUM_LARGE" />
            <el-option label="全部杯型" value="ALL_SIZES" />
          </el-select>
          <div class="form-hint-inline">DEFAULT=仅中杯</div>
        </el-form-item>
      </el-col>

      <el-col :span="8">
        <el-form-item label="糖度选项">
          <el-select v-model="form.sugarType" placeholder="选择规格" style="width: 100%">
            <el-option label="自由选择" value="FREE_CHOICE" />
            <el-option label="仅无糖" value="NO_SUGAR_ONLY" />
            <el-option label="最甜少甜" value="MIN_LESS_SWEET" />
          </el-select>
          <div class="form-hint-inline">FREE_CHOICE=全糖度可选</div>
        </el-form-item>
      </el-col>

      <el-col :span="8">
        <el-form-item label="温度选项">
          <el-select v-model="form.tempType" placeholder="选择规格" style="width: 100%">
            <el-option label="热/冰" value="HOT_COLD" />
            <el-option label="仅冰" value="COLD_ONLY" />
            <el-option label="仅热" value="HOT_ONLY" />
          </el-select>
          <div class="form-hint-inline">Dirty类推荐COLD_ONLY</div>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="默认甜度">
          <el-select
            v-model="form.defaultSugarLevel"
            placeholder="默认额外加糖等级"
            style="width: 100%"
            :disabled="form.sugarType === 'NO_SUGAR_ONLY'"
          >
            <el-option label="标准糖" value="STANDARD" />
            <el-option label="少糖" value="LESS" />
            <el-option label="半糖" value="HALF" />
            <el-option label="不另外加糖" value="NO_ADDED_SUGAR" />
          </el-select>
          <div class="form-hint-inline">澳白/美式默认「不另外加糖」；NO_SUGAR_ONLY 商品留空</div>
        </el-form-item>
      </el-col>
    </el-row>
  </div>

  <!-- 豆/拼配挂接（V2：咖啡类必选，bean_id / blend_id 二选一） -->
  <div v-if="isCoffeeCategory" class="form-section">
    <div class="section-title">
      <el-icon><Coffee /></el-icon>
      <span>豆/拼配</span>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #title>咖啡类商品必须挂单品豆或拼配豆（二选一），移动端豆档案由此渲染</template>
    </el-alert>

    <el-form-item label="豆/拼配">
      <el-select
        v-model="beanBlendValue"
        placeholder="选择单品豆或拼配豆"
        filterable
        clearable
        style="width: 100%"
      >
        <el-option-group label="单品豆">
          <el-option v-for="b in activeBeans" :key="'bean-' + b.id" :value="'bean:' + b.id" :label="`${b.name}（${b.code}）`" />
        </el-option-group>
        <el-option-group label="拼配豆">
          <el-option v-for="bl in activeBlends" :key="'blend-' + bl.id" :value="'blend:' + bl.id" :label="`${bl.name}（${bl.code}）`" />
        </el-option-group>
      </el-select>
      <div class="form-hint-inline">单品豆/拼配豆二选一；非咖啡/烘焙无需选择</div>
    </el-form-item>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { InfoFilled, Setting, Coffee } from '@element-plus/icons-vue'
import ImageUploader from './ImageUploader.vue'
import { PRODUCT_TAG_OPTIONS as tagOptions } from '@/constants/product'

const props = defineProps({
  form: { type: Object, required: true },
  beans: { type: Array, default: () => [] },
  blends: { type: Array, default: () => [] }
})

// V2 咖啡系列分类：需挂 bean_id / blend_id（二选一）
const COFFEE_CATEGORIES = ['ESPRESSO', 'MILK', 'SIGNATURE', 'SPECIALTY']
const isCoffeeCategory = computed(() => COFFEE_CATEGORIES.includes(props.form.category))

const activeBeans = computed(() => props.beans.filter(b => b.status === 'active'))
const activeBlends = computed(() => props.blends.filter(b => b.status === 'active'))

// bean_id / blend_id 二选一：'bean:1' | 'blend:2' | ''
const beanBlendValue = computed({
  get: () => {
    const f = props.form
    if (f.beanId) return 'bean:' + f.beanId
    if (f.blendId) return 'blend:' + f.blendId
    return ''
  },
  set: (v) => {
    const f = props.form
    if (v && v.startsWith('bean:')) { f.beanId = Number(v.slice(5)); f.blendId = null }
    else if (v && v.startsWith('blend:')) { f.blendId = Number(v.slice(6)); f.beanId = null }
    else { f.beanId = null; f.blendId = null }
  }
})

// NO_SUGAR_ONLY 商品无糖度配置 → default_sugar_level 必须 NULL
watch(() => props.form.sugarType, (v) => {
  if (v === 'NO_SUGAR_ONLY') props.form.defaultSugarLevel = null
})
</script>

<style scoped lang="scss">
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

.form-hint-inline {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
</style>
