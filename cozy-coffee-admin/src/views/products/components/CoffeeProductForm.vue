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
        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
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
          <ImageUploader v-model="form.imageUrl" />
        </el-form-item>
      </el-col>

      <!-- Espresso: medium + large -->
      <template v-if="form.category === 'espresso'">
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
          <el-form-item label="大杯价格(元)">
            <el-input-number
              v-model="form.priceLarge"
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

      <!-- Non-espresso: standard price only -->
      <template v-else>
        <el-col :span="12">
          <el-form-item label="标准价格(元)" required>
            <el-input-number
              v-model="form.priceMedium"
              :min="0"
              :precision="2"
              :step="0.5"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入价格"
            />
            <div v-if="form.category === 'addon'" class="form-hint-inline">如：额外浓缩 5元</div>
          </el-form-item>
        </el-col>
      </template>

      <el-col :span="12">
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option label="☕ 意式咖啡" value="espresso" />
            <el-option label="⭐ 季节限定" value="signature" />
            <el-option label="✨ 精品手冲" value="soe" />
            <el-option label="🍰 烘焙甜品" value="bakery" />
            <el-option label="➕ 加料/配料" value="addon" />
            <el-option label="📦 其他" value="other" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item label="新品标识">
          <el-switch v-model="form.isNewProduct" active-text="是" inactive-text="否" />
          <div class="form-hint-inline">标记为"New"新品，显示在菜单顶部</div>
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
</template>

<script setup>
import { InfoFilled, Setting } from '@element-plus/icons-vue'
import ImageUploader from './ImageUploader.vue'

defineProps({
  form: { type: Object, required: true }
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
