<template><EmptyState icon="!" :title="title" :description="description" action-text="重新加载" @action="$emit('retry')" /></template>
<script setup>import EmptyState from './EmptyState.vue';defineProps({title:{type:String,default:'加载失败'},description:{type:String,default:'请检查网络后重试'}});defineEmits(['retry'])</script>
