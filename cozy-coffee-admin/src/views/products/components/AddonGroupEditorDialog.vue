<template>
  <el-dialog
    :model-value="visible"
    width="960px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <template #header>
      <div class="editor-header">
        <span class="editor-title">加料组配置</span>
        <span class="editor-sub">{{ product?.name || '' }}</span>
      </div>
    </template>

    <div v-if="loading" v-loading="true" class="editor-loading" />

    <div v-else class="editor-layout">
      <!-- 左：加料组列表 -->
      <div class="groups-area">
        <el-empty
          v-if="groups.length === 0"
          description="暂无加料组，点击下方添加"
          :image-size="80"
        />

        <div v-for="(group, gi) in groups" :key="gi" class="group-card">
          <div class="group-card-header">
            <el-tag size="small" effect="dark" class="group-cat-tag">{{ group.category }}</el-tag>
            <el-button link type="danger" size="small" @click="removeGroup(gi)">
              <el-icon><Delete /></el-icon>删除组
            </el-button>
          </div>

          <div class="group-settings">
            <div class="field">
              <div class="field-label">类别</div>
              <el-select
                v-model="group.category"
                size="small"
                style="width: 100%"
                @change="onGroupCategoryChange(gi)"
              >
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </div>
            <div class="field">
              <div class="field-label">选择模式</div>
              <el-select v-model="group.selectionMode" size="small" style="width: 100%" @change="onSelectionModeChange(group)">
                <el-option label="单选 SINGLE" value="SINGLE" />
                <el-option label="多选 MULTI" value="MULTI" />
              </el-select>
            </div>
            <div class="field field-sm">
              <div class="field-label">必选</div>
              <el-switch
                :model-value="group.minSelect > 0"
                size="small"
                @change="(v) => onRequiredChange(group, v)"
              />
              <div class="field-hint">{{ group.minSelect > 0 ? '未选自动注入默认项' : '可不选' }}</div>
            </div>
            <div class="field field-sm">
              <div class="field-label">上限</div>
              <el-input-number
                v-if="group.selectionMode === 'MULTI'"
                v-model="group.maxSelect"
                size="small"
                :min="1"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
              <el-input-number
                v-else
                :model-value="1"
                disabled
                size="small"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-hint">{{ group.selectionMode === 'SINGLE' ? '单选互斥' : '多选上限' }}</div>
            </div>
            <div class="field field-sm">
              <div class="field-label">排序</div>
              <el-input-number
                v-model="group.sortOrder"
                size="small"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </div>
          </div>

          <div class="items-wrap">
            <div class="items-head">
              <span class="items-title">组内项（{{ group.items.length }}）</span>
              <el-button size="small" type="primary" plain @click="addItem(gi)">
                <el-icon class="el-icon--left"><Plus /></el-icon>添加加料
              </el-button>
            </div>

            <template v-if="group.items.length">
              <div class="item-grid item-grid-head">
                <span class="cell-head">加料</span>
                <span class="cell-head">默认</span>
                <span class="cell-head">增量(元)</span>
                <span class="cell-head">排序</span>
                <span />
              </div>

              <div v-for="(item, ii) in group.items" :key="ii" class="item-grid item-row">
                <el-select
                  :model-value="item.addonId"
                  size="small"
                  filterable
                  placeholder="选择加料"
                  style="width: 100%"
                  @change="(v) => onItemAddonChange(gi, ii, v)"
                >
                  <el-option
                    v-for="opt in itemOptions(group)"
                    :key="opt.id"
                    :value="opt.id"
                    :label="opt.name + '（' + opt.code + '）'"
                  >
                    <span>{{ opt.name }}（{{ opt.code }}）</span>
                    <span class="opt-right">{{ optLabel(opt) }}</span>
                  </el-option>
                </el-select>

                <el-switch
                  v-model="item.isDefault"
                  size="small"
                  @change="onDefaultChange(item)"
                />

                <el-input-number
                  v-model="item.priceDelta"
                  size="small"
                  :min="0"
                  :precision="2"
                  :step="0.5"
                  :disabled="!!item.isDefault"
                  controls-position="right"
                  style="width: 100%"
                />

                <el-input-number
                  v-model="item.sortOrder"
                  size="small"
                  :precision="0"
                  controls-position="right"
                  style="width: 100%"
                />

                <el-button link type="danger" size="small" @click="removeItem(gi, ii)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </template>
            <div v-else class="items-empty">暂无组内项，请添加加料</div>
          </div>
        </div>

        <el-button class="add-group-btn" @click="addGroup">
          <el-icon class="el-icon--left"><Plus /></el-icon>添加加料组
        </el-button>
      </div>

      <!-- 右：加料目录（参考增量） -->
      <div class="catalog-panel">
        <div class="catalog-title">
          加料目录
          <span class="catalog-hint">参考增量</span>
        </div>
        <el-scrollbar max-height="380px">
          <div v-for="cat in catalogByCat" :key="cat.category" class="catalog-cat">
            <div class="catalog-cat-name">{{ cat.category }}</div>
            <div v-for="a in cat.items" :key="a.id" class="catalog-item">
              <span class="catalog-item-name">{{ a.name }}</span>
              <span class="catalog-item-code">{{ a.code }}</span>
              <span class="catalog-item-price">{{ optLabel(a) }}</span>
            </div>
          </div>
          <el-empty v-if="catalog.length === 0" description="暂无可用加料" :image-size="60" />
        </el-scrollbar>
        <div class="catalog-foot">
          绑定加料时预填「参考增量」，实际价格以各商品「增量」为准
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button size="large" @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" size="large" :loading="saving" @click="handleSave">
          <el-icon class="el-icon--left"><Check /></el-icon>保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Check, Delete } from '@element-plus/icons-vue'
import { getAddonCatalog, saveAddonGroups } from '@/api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  product: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'saved'])

const loading = ref(false)
const saving = ref(false)
const catalog = ref([])
const catalogById = computed(() => new Map(catalog.value.map(a => [a.id, a])))
const groups = ref([])

const KNOWN_CATEGORIES = ['MILK', 'SHOT', 'SYRUP', 'OTHER']

const categoryOptions = computed(() => {
  const fromCatalog = [...new Set(catalog.value.map(a => a.category).filter(Boolean))]
  return fromCatalog.length ? fromCatalog : KNOWN_CATEGORIES
})

const catalogByCat = computed(() => {
  const map = new Map()
  catalog.value.forEach(a => {
    if (!map.has(a.category)) map.set(a.category, [])
    map.get(a.category).push(a)
  })
  const order = [...KNOWN_CATEGORIES, ...map.keys()]
  return [...map.entries()]
    .sort((x, y) => order.indexOf(x[0]) - order.indexOf(y[0]))
    .map(([category, items]) => ({ category, items }))
})

const optLabel = (a) =>
  a.inactive ? '已停用' : (a.price != null && a.price > 0 ? `+¥${a.price}` : '免费')

// 组内可选项：该类别 active 加料 + 已绑定但已停用的项（保留显示，禁止保存）
function itemOptions(group) {
  const opts = catalog.value.filter(a => a.category === group.category).slice()
  const ids = new Set(opts.map(a => a.id))
  group.items.forEach(it => {
    if (it.addonId && !ids.has(it.addonId)) {
      opts.unshift({ id: it.addonId, code: it.code, name: it.name, price: it.price, inactive: true })
      ids.add(it.addonId)
    }
  })
  return opts
}

function buildGroups() {
  const src = props.product?.addonGroups || []
  groups.value = src.map(g => {
    const mode = g.selectionMode === 'MULTI' ? 'MULTI' : 'SINGLE'
    return {
      category: g.category,
      selectionMode: mode,
      minSelect: g.minSelect === 1 ? 1 : 0,
      maxSelect: mode === 'SINGLE' ? 1 : Math.max(1, g.maxSelect ?? 1),
      sortOrder: g.sortOrder ?? 0,
      items: (g.items || []).map(it => {
        const addon = catalogById.value.get(it.addonId)
        return {
          addonId: it.addonId,
          code: it.code,
          name: it.name,
          price: addon?.price,
          isDefault: !!it.isDefault,
          priceDelta: it.priceDelta != null ? Number(it.priceDelta) : 0,
          sortOrder: it.sortOrder ?? 0,
          inactive: !addon
        }
      })
    }
  })
}

async function open() {
  loading.value = true
  try {
    const res = await getAddonCatalog()
    catalog.value = res.data || []
    buildGroups()
  } catch (e) {
    ElMessage.error('加载加料目录失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (v) => { if (v) open() })

// ==================== 组操作 ====================

function addGroup() {
  groups.value.push({
    category: categoryOptions.value[0] || 'MILK',
    selectionMode: 'SINGLE',
    minSelect: 0,
    maxSelect: 1,
    sortOrder: groups.value.length + 1,
    items: []
  })
}

function removeGroup(gi) {
  groups.value.splice(gi, 1)
}

function onGroupCategoryChange(gi) {
  const group = groups.value[gi]
  const before = group.items.length
  group.items = group.items.filter(it => {
    const addon = catalogById.value.get(it.addonId)
    return addon && addon.category === group.category
  })
  if (group.items.length < before) {
    ElMessage.warning('已移除不匹配该类别的加料 ' + (before - group.items.length) + ' 项')
  }
}

// SINGLE 单选互斥 = 上限固定 1；仅 MULTI 开放多选上限
function onSelectionModeChange(group) {
  if (group.selectionMode !== 'MULTI') group.maxSelect = 1
}

// 必选 = min_select 0/1（未选自动注入默认项）
function onRequiredChange(group, required) {
  group.minSelect = required ? 1 : 0
}

function addItem(gi) {
  const group = groups.value[gi]
  group.items.push({
    addonId: null,
    code: '',
    name: '',
    price: 0,
    isDefault: false,
    priceDelta: 0,
    sortOrder: group.items.length + 1
  })
}

function removeItem(gi, ii) {
  groups.value[gi].items.splice(ii, 1)
}

function onItemAddonChange(gi, ii, newId) {
  if (!newId) return
  const item = groups.value[gi].items[ii]
  if (item.addonId === newId) return
  const addon = catalogById.value.get(newId)
  if (!addon) return
  item.addonId = newId
  item.code = addon.code
  item.name = addon.name
  item.price = addon.price
  if (!item.isDefault) item.priceDelta = addon.price != null ? Number(addon.price) : 0
}

function onDefaultChange(item) {
  if (item.isDefault) item.priceDelta = 0
}

// ==================== 校验 + 保存 ====================

function validate() {
  for (const g of groups.value) {
    if (!g.category) { ElMessage.warning('加料组类别不能为空'); return false }
    if (!['SINGLE', 'MULTI'].includes(g.selectionMode)) { ElMessage.warning('选择模式非法'); return false }
    if (g.minSelect > g.maxSelect) { ElMessage.warning('加料组「' + g.category + '」必选与多选上限冲突'); return false }
    if (g.items.length === 0) {
      if (g.maxSelect > 0) { ElMessage.warning('加料组「' + g.category + '」暂无组内项'); return false }
      continue
    }
    if (g.maxSelect > g.items.length) { ElMessage.warning('加料组「' + g.category + '」最多选择超过组内项数'); return false }
    const defaults = g.items.filter(i => i.isDefault)
    if (g.minSelect > 0 && defaults.length === 0) { ElMessage.warning('加料组「' + g.category + '」必选但未设置默认项'); return false }
    if (defaults.length > g.maxSelect) { ElMessage.warning('加料组「' + g.category + '」默认项数量超过最多选择'); return false }
    if (g.selectionMode === 'SINGLE' && g.minSelect > 0 && defaults.length !== 1) {
      ElMessage.warning('加料组「' + g.category + '」SINGLE 必选组默认项必须为 1'); return false
    }
    for (const it of g.items) {
      if (!it.addonId) { ElMessage.warning('加料组「' + g.category + '」存在未选择加料的项'); return false }
      const addon = catalogById.value.get(it.addonId)
      if (!addon) { ElMessage.warning('加料组「' + g.category + '」存在已停用加料，请先移除：' + (it.name || it.addonId)); return false }
      if (addon.category !== g.category) { ElMessage.warning('加料「' + it.name + '」类别与组不匹配'); return false }
      if (it.priceDelta == null || it.priceDelta < 0) { ElMessage.warning('加料「' + it.name + '」增量禁止负值'); return false }
      if (it.isDefault && it.priceDelta !== 0) { ElMessage.warning('默认项「' + it.name + '」增量必须为 0'); return false }
    }
  }
  return true
}

async function handleSave() {
  if (!props.product?.id) return
  if (!validate()) return

  const payload = groups.value.map(g => ({
    category: g.category,
    selectionMode: g.selectionMode,
    minSelect: g.minSelect,
    maxSelect: g.maxSelect,
    sortOrder: g.sortOrder,
    items: g.items.map(it => ({
      addonId: it.addonId,
      isDefault: it.isDefault,
      priceDelta: it.priceDelta,
      sortOrder: it.sortOrder
    }))
  }))

  saving.value = true
  try {
    await saveAddonGroups(props.product.id, payload)
    ElMessage.success('加料组配置已保存')
    emit('update:visible', false)
    emit('saved')
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.editor-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.editor-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.editor-sub {
  font-size: 13px;
  color: #909399;
}

.editor-loading {
  min-height: 320px;
}

.editor-layout {
  display: flex;
  gap: 20px;
  min-height: 320px;
}

.groups-area {
  flex: 1;
  min-width: 0;
}

.group-card {
  border: 1px solid #E5E7EB;
  border-radius: 6px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #FAFAFA;

  &:last-of-type {
    margin-bottom: 12px;
  }
}

.group-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.group-cat-tag {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.group-settings {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.field {
  flex: 1;
  min-width: 0;

  &.field-sm {
    flex: 0 0 96px;
  }
}

.field-label {
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}

.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.3;
  white-space: nowrap;
}

.items-wrap {
  background: #fff;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 10px 12px;
}

.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.items-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.items-empty {
  padding: 18px 0;
  text-align: center;
  color: #909399;
  font-size: 12px;
}

.item-grid {
  display: grid;
  grid-template-columns: 1fr 64px 110px 84px 36px;
  gap: 8px;
  align-items: center;
}

.item-grid-head {
  margin-bottom: 4px;
}

.cell-head {
  font-size: 12px;
  color: #909399;
}

.item-row {
  padding: 4px 0;

  + .item-row {
    border-top: 1px dashed #F0F2F5;
  }
}

.opt-right {
  float: right;
  color: #909399;
  font-size: 12px;
}

.add-group-btn {
  width: 100%;
  border-style: dashed;
}

// 右：加料目录
.catalog-panel {
  flex: 0 0 260px;
  border-left: 1px solid #EBEEF5;
  padding-left: 20px;
}

.catalog-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.catalog-hint {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.catalog-cat {
  margin-bottom: 12px;
}

.catalog-cat-name {
  font-size: 12px;
  font-weight: 600;
  color: #8B5E3C;
  margin-bottom: 6px;
}

.catalog-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-size: 12px;
}

.catalog-item-name {
  color: #303133;
}

.catalog-item-code {
  color: #B0B3B8;
  font-size: 11px;
}

.catalog-item-price {
  margin-left: auto;
  color: #8B5E3C;
  font-weight: 600;
  white-space: nowrap;
}

.catalog-foot {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #E5E7EB;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
