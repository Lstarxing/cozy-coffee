<template>
  <view v-if="isDev" class="dev-switcher">
    <view class="dev-fab" @click="open = true">
      <text class="dev-fab-glyph">⚙️</text>
    </view>
    <view v-if="open" class="dev-mask" @click="open = false">
      <view class="dev-sheet" @click.stop>
        <view class="dev-head">
          <text class="dev-title">开发模式 · 当前: {{ currentLabel }}</text>
          <text class="dev-close" @click="open = false">×</text>
        </view>
        <text class="dev-source">来源: {{ store.devOverride ? '● DEV MOCK' : '○ API' }}</text>
        <text class="dev-meta">EXP: {{ memberInfo.expTotal }} · 积分: {{ memberInfo.currentPoints }}</text>
        <view class="dev-grid">
          <view
            v-for="lvl in LEVELS"
            :key="lvl"
            class="dev-btn"
            :class="[lvl, { active: currentLevel === lvl }]"
            @click="select(lvl)"
          >
            <text>{{ LABELS[lvl] }}</text>
          </view>
        </view>
        <view class="dev-restore" @click="restore">
          <text>恢复真实等级</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useSessionStore } from '@/stores/session'
import { MEMBER_LEVELS, MEMBER_LEVEL_NAMES } from '@/constants/member'

const isDev = import.meta.env.DEV
const store = useSessionStore()
const open = ref(false)
const LEVELS = MEMBER_LEVELS
const LABELS = MEMBER_LEVEL_NAMES
const currentLevel = computed(() => store.userLevel)
const memberInfo = computed(() => store.memberInfo)
const currentLabel = computed(() => MEMBER_LEVEL_NAMES[currentLevel.value] || currentLevel.value)

function select(lvl) {
  store.setDevLevel(lvl)
  open.value = false
}
function restore() {
  store.clearDevOverride()
  store.refreshMemberInfo()
  open.value = false
}
</script>

<style lang="scss" scoped>
.dev-switcher { position: fixed; z-index: 9999; }
.dev-fab {
  position: fixed; right: 32rpx; bottom: calc(180rpx + env(safe-area-inset-bottom));
  width: 88rpx; height: 88rpx; border-radius: 50%;
  background: rgba(44, 30, 24, 0.88); display: flex; align-items: center; justify-content: center;
  box-shadow: $cozy-shadow-raised;
}
.dev-fab-glyph { font-size: 40rpx; }
.dev-mask { position: fixed; inset: 0; background: $cozy-overlay; z-index: 9998; display: flex; align-items: flex-end; }
.dev-sheet {
  width: 100%; background: $cozy-bg; border-radius: $cozy-radius-lg $cozy-radius-lg 0 0;
  padding: 32rpx 28rpx calc(48rpx + env(safe-area-inset-bottom));
}
.dev-head { display: flex; align-items: center; justify-content: space-between; }
.dev-title { font-size: $font-size-md; font-weight: 700; color: $cozy-ink; }
.dev-close { font-size: 44rpx; color: $cozy-muted; padding: 0 12rpx; line-height: 1; }
.dev-source { display: block; margin-top: 12rpx; font-size: $font-size-xs; color: $cozy-muted; }
.dev-meta { display: block; margin-top: 4rpx; font-size: $font-size-xs; color: $cozy-muted; }
.dev-grid { margin-top: 24rpx; display: flex; flex-wrap: wrap; gap: 16rpx; }
.dev-btn {
  flex: 1 1 calc(33.333% - 16rpx); min-width: 180rpx; height: 88rpx;
  border-radius: $cozy-radius-md; display: flex; align-items: center; justify-content: center;
  background: $cozy-surface; color: $cozy-muted; font-size: $font-size-sm; font-weight: 600;
}
.dev-btn.active { color: #fff; }
.dev-btn.basic.active { background: #8D6E63; }
.dev-btn.silver.active { background: #8C7B70; }
.dev-btn.gold.active { background: #B8862D; }
.dev-btn.diamond.active { background: #546E7A; }
.dev-btn.black.active { background: #171411; color: #E6C97A; }
.dev-restore {
  margin-top: 24rpx; height: 88rpx; border-radius: $cozy-radius-md;
  border: 1rpx solid $cozy-border; display: flex; align-items: center; justify-content: center;
  color: $cozy-ink; font-size: $font-size-sm;
}
</style>
