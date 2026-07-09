<template>
  <div class="form-section">
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
          <el-radio-group v-model="form.couponType" size="default">
            <el-radio-button value="EXCHANGE">兑换券</el-radio-button>
            <el-radio-button value="DISCOUNT">折扣券</el-radio-button>
            <el-radio-button value="FULL_REDUCE">满减券</el-radio-button>
            <el-radio-button value="BOGO">买一送一</el-radio-button>
            <el-radio-button value="SHOT">加浓缩</el-radio-button>
            <el-radio-button value="DELIVERY_FEE">配送费</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-col>

      <!-- Exchange coupon -->
      <template v-if="form.couponType === 'EXCHANGE'">
        <el-col :span="24">
          <el-form-item label="可兑换饮品">
            <el-select
              v-model="form.linkedProductId"
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
            <div v-if="!form.linkedProductId" class="form-hint-inline">
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

      <!-- Discount coupon -->
      <el-col v-if="form.couponType === 'DISCOUNT'" :span="24">
        <el-form-item label="折扣率" required>
          <el-slider
            v-model="form.couponValue"
            :min="10"
            :max="99"
            :step="5"
            show-input
            :format-tooltip="(val) => `${val / 10}折`"
          />
          <div class="form-hint-block">
            当前折扣：<strong>{{ (form.couponValue / 10).toFixed(1) }}折</strong>
          </div>
        </el-form-item>
      </el-col>

      <!-- Full-reduce coupon -->
      <template v-if="form.couponType === 'FULL_REDUCE'">
        <el-col :span="12">
          <el-form-item label="满减门槛(元)">
            <el-input-number
              v-model="form.minOrderAmount"
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
              v-model="form.couponValue"
              :min="1"
              :max="999"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <div class="form-hint-block">
            优惠规则：订单满 <strong>¥{{ form.minOrderAmount || 0 }}</strong> 可减 <strong>¥{{ form.couponValue || 0 }}</strong>
          </div>
        </el-col>
      </template>

      <!-- BOGO coupon -->
      <template v-if="form.couponType === 'BOGO'">
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

      <!-- SHOT coupon -->
      <template v-if="form.couponType === 'SHOT'">
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

      <!-- Delivery Fee coupon -->
      <template v-if="form.couponType === 'DELIVERY_FEE'">
        <el-col :span="12">
          <el-form-item label="最高抵扣(元)">
            <el-input-number
              v-model="form.couponValue"
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

  <!-- Common Settings -->
  <div class="form-section">
    <div class="section-title settings">
      <el-icon><Setting /></el-icon>
      <span>通用配置</span>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="月度限购">
          <el-input-number
            v-model="form.monthlyLimit"
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

      <el-col v-if="form.category === 'coupon'" :span="12">
        <el-form-item label="券有效期(天)">
          <el-input-number
            v-model="form.validDays"
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
</template>

<script setup>
import { Ticket, Setting } from '@element-plus/icons-vue'

defineProps({
  form: { type: Object, required: true },
  coffeeProducts: { type: Array, default: () => [] }
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

  &.coupon {
    background-color: #fff0f6;
    border-left-color: #eb2f96;
    color: #303133;

    .el-icon { color: #eb2f96; }
  }

  &.settings {
    background-color: #e6f7ff;
    border-left-color: #1890ff;
    color: #303133;

    .el-icon { color: #1890ff; }
  }
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
</style>
