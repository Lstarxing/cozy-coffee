<template><EmptyState icon="断" title="当前网络不可用" description="购物车会保留，恢复连接后可继续提交" action-text="重新连接" @action="$emit('retry')" /></template>
<script setup>import EmptyState from './EmptyState.vue';defineEmits(['retry'])</script>
