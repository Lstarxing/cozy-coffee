<template>
  <div class="archives-page">
    <AdminPageHeader title="内容档案" subtitle="产区 · 单品豆 · 拼配豆 · 咖啡豆内容单一事实来源" />

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ── 产区 Origin ─────────────────────────────── -->
        <el-tab-pane label="产区 Origin" name="origin">
          <div class="tab-toolbar">
            <span class="tab-count">共 {{ origins.length }} 个产区</span>
            <el-button type="primary" size="default" @click="openOriginDialog()">
              <el-icon class="el-icon--left"><Plus /></el-icon>新增产区
            </el-button>
          </div>
          <el-table :data="origins" size="small" v-loading="originLoading">
            <el-table-column prop="code" label="代码" width="110" />
            <el-table-column prop="country" label="国家（英）" min-width="100" />
            <el-table-column prop="countryZh" label="国家（中）" width="110" />
            <el-table-column prop="region" label="产区/子区域" min-width="130" />
            <el-table-column prop="typicalCharacter" label="典型气质" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openOriginDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该产区？" @confirm="handleDeleteOrigin(row)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ── 单品豆 Bean ─────────────────────────────── -->
        <el-tab-pane label="单品豆 Bean" name="bean">
          <div class="tab-toolbar">
            <span class="tab-count">共 {{ beans.length }} 支单品豆</span>
            <el-button type="primary" size="default" @click="openBeanDialog()">
              <el-icon class="el-icon--left"><Plus /></el-icon>新增单品豆
            </el-button>
          </div>
          <el-table :data="beans" size="small" v-loading="beanLoading">
            <el-table-column prop="code" label="代码" width="140" />
            <el-table-column prop="name" label="豆名" min-width="100" />
            <el-table-column prop="nameEn" label="英文名" min-width="110" />
            <el-table-column prop="originName" label="产区" width="110" />
            <el-table-column prop="roast" label="烘焙" width="120" show-overflow-tooltip />
            <el-table-column prop="flavorNotes" label="风味" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openBeanDialog(row)">编辑</el-button>
                <el-popconfirm title="删除后不可恢复，确定？" @confirm="handleDeleteBean(row)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ── 拼配豆 Blend ────────────────────────────── -->
        <el-tab-pane label="拼配豆 Blend" name="blend">
          <div class="tab-toolbar">
            <span class="tab-count">共 {{ blends.length }} 支拼配</span>
            <el-button type="primary" size="default" @click="openBlendDialog()">
              <el-icon class="el-icon--left"><Plus /></el-icon>新增拼配豆
            </el-button>
          </div>
          <el-table :data="blends" size="small" v-loading="blendLoading">
            <el-table-column prop="code" label="代码" width="130" />
            <el-table-column prop="name" label="拼配名" min-width="100" />
            <el-table-column prop="nameEn" label="英文名" min-width="110" />
            <el-table-column prop="roast" label="烘焙" width="110" show-overflow-tooltip />
            <el-table-column label="拼配" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ compositionText(row.composition) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openBlendDialog(row)">编辑</el-button>
                <el-popconfirm title="删除后不可恢复，确定？" @confirm="handleDeleteBlend(row)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <OriginFormDialog v-model:visible="originVisible" :form="originForm" :saving="saving" @confirm="saveOrigin" />
    <BeanFormDialog v-model:visible="beanVisible" :form="beanForm" :origins="origins" :saving="saving" @confirm="saveBean" />
    <BlendFormDialog v-model:visible="blendVisible" :form="blendForm" :beans="beans" :saving="saving" @confirm="saveBlend" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/ui/AdminPageHeader.vue'
import OriginFormDialog from './components/OriginFormDialog.vue'
import BeanFormDialog from './components/BeanFormDialog.vue'
import BlendFormDialog from './components/BlendFormDialog.vue'
import {
  getOrigins, saveOrigin as apiSaveOrigin, deleteOrigin as apiDeleteOrigin,
  getBeans, saveBean as apiSaveBean, deleteBean as apiDeleteBean,
  getBlends, saveBlend as apiSaveBlend, deleteBlend as apiDeleteBlend
} from '@/api'

const activeTab = ref('origin')
const origins = ref([])
const beans = ref([])
const blends = ref([])
const originLoading = ref(false)
const beanLoading = ref(false)
const blendLoading = ref(false)
const saving = ref(false)

// ── 加载 ─────────────────────────────────────────────

const loadOrigins = async () => {
  originLoading.value = true
  try { const res = await getOrigins(); origins.value = res.data || [] }
  catch (e) { ElMessage.error('加载产区失败: ' + e.message) }
  finally { originLoading.value = false }
}
const loadBeans = async () => {
  beanLoading.value = true
  try { const res = await getBeans(); beans.value = res.data || [] }
  catch (e) { ElMessage.error('加载单品豆失败: ' + e.message) }
  finally { beanLoading.value = false }
}
const loadBlends = async () => {
  blendLoading.value = true
  try { const res = await getBlends(); blends.value = res.data || [] }
  catch (e) { ElMessage.error('加载拼配豆失败: ' + e.message) }
  finally { blendLoading.value = false }
}

onMounted(() => { loadOrigins(); loadBeans(); loadBlends() })

// ── 产区 ─────────────────────────────────────────────

const originVisible = ref(false)
const originForm = ref({})

const emptyOrigin = () => ({ code: '', country: '', countryZh: '', region: '', typicalCharacter: '', description: '', sortOrder: 0, status: 'active' })
const openOriginDialog = (row) => {
  originForm.value = row ? { ...row } : emptyOrigin()
  originVisible.value = true
}
const saveOrigin = async () => {
  if (!originForm.value.code || !originForm.value.country) { ElMessage.warning('代码与国家必填'); return }
  saving.value = true
  try { await apiSaveOrigin(originForm.value); ElMessage.success('产区已保存'); originVisible.value = false; loadOrigins() }
  catch (e) { ElMessage.error('保存失败: ' + e.message) }
  finally { saving.value = false }
}
const handleDeleteOrigin = async (row) => {
  try { await apiDeleteOrigin(row.id); ElMessage.success('已删除'); loadOrigins() }
  catch (e) { ElMessage.error(e.message) }
}

// ── 单品豆 ───────────────────────────────────────────

const beanVisible = ref(false)
const beanForm = ref({})

const emptyBean = () => ({ code: '', name: '', nameEn: '', originId: null, altitude: '', processing: '', variety: '', roast: '', flavorNotes: '', body: '', acidity: '', role: '', description: '', sortOrder: 0, status: 'active' })
const openBeanDialog = (row) => {
  beanForm.value = row ? { ...row } : emptyBean()
  beanVisible.value = true
}
const saveBean = async () => {
  if (!beanForm.value.code || !beanForm.value.name) { ElMessage.warning('代码与豆名必填'); return }
  saving.value = true
  try { await apiSaveBean(beanForm.value); ElMessage.success('单品豆已保存'); beanVisible.value = false; loadBeans() }
  catch (e) { ElMessage.error('保存失败: ' + e.message) }
  finally { saving.value = false }
}
const handleDeleteBean = async (row) => {
  try { await apiDeleteBean(row.id); ElMessage.success('已删除'); loadBeans() }
  catch (e) { ElMessage.error(e.message) }
}

// ── 拼配豆 ───────────────────────────────────────────

const blendVisible = ref(false)
const blendForm = ref({})

const emptyBlend = () => ({ code: '', name: '', nameEn: '', description: '', composition: [], roast: '', flavorNotes: '', body: '', acidity: '', sortOrder: 0, status: 'active' })
const openBlendDialog = (row) => {
  blendForm.value = row ? { ...row, composition: (row.composition || []).map(c => ({ ...c })) } : emptyBlend()
  blendVisible.value = true
}
const saveBlend = async () => {
  const composition = blendForm.value.composition || []
  const total = composition.reduce((s, c) => s + Number(c.ratio || 0), 0)
  if (composition.length === 0) { ElMessage.warning('请添加拼配项'); return }
  if (composition.some(c => !c.beanId)) { ElMessage.warning('存在未选择的单品豆'); return }
  if (total !== 100) { ElMessage.warning(`拼配比例合计需为 100，当前 ${total}`); return }
  saving.value = true
  try { await apiSaveBlend(blendForm.value); ElMessage.success('拼配豆已保存'); blendVisible.value = false; loadBlends() }
  catch (e) { ElMessage.error('保存失败: ' + e.message) }
  finally { saving.value = false }
}
const handleDeleteBlend = async (row) => {
  try { await apiDeleteBlend(row.id); ElMessage.success('已删除'); loadBlends() }
  catch (e) { ElMessage.error(e.message) }
}

// ── 工具 ─────────────────────────────────────────────

const compositionText = (composition) => {
  if (!Array.isArray(composition) || composition.length === 0) return '—'
  const byId = Object.fromEntries(beans.value.map(b => [b.id, b.name]))
  return composition.map(c => `${byId[c.beanId] || c.beanId} ${c.ratio}%`).join(' + ')
}
</script>

<style scoped>
.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.tab-count {
  font-size: 13px;
  color: #6B7280;
}
</style>
