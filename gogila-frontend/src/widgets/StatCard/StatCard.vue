<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import http from '../../api/http';
import type { DataBinding } from '../../types/screen';

interface Props {
  title?: string;
  unit?: string;
  color?: string;
  dataBinding?: DataBinding;
}

const props = defineProps<Props>();

const value = ref<number | null>(null);
const loading = ref(false);

async function fetchData() {
  if (!props.dataBinding || props.dataBinding.mode !== 'http' || !props.dataBinding.api) {
    return;
  }
  loading.value = true;
  try {
    const resp = await http.get(props.dataBinding.api.replace('/api', ''));
    // 这里假设返回 { value: number }
    value.value = resp.data.value ?? null;
  } catch (e) {
    console.error('StatCard 获取数据失败', e);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchData();

  // 简单轮询示例
  if (props.dataBinding?.interval && props.dataBinding.interval > 0) {
    setInterval(() => {
      fetchData();
    }, props.dataBinding.interval);
  }
});

watch(
    () => props.dataBinding,
    () => {
      fetchData();
    }
);
</script>

<template>
  <div class="stat-card" :style="{ borderColor: color || '#38bdf8' }">
    <div class="stat-title">{{ title || '指标' }}</div>
    <div class="stat-value">
      <span v-if="loading">...</span>
      <span v-else>{{ value ?? '-' }}</span>
      <span v-if="unit" class="stat-unit">{{ unit }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  width: 100%;
  height: 100%;
  border: 1px solid;
  border-radius: 8px;
  padding: 12px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: rgba(15, 23, 42, 0.9);
  color: #e5e7eb;
}

.stat-title {
  font-size: 14px;
  opacity: 0.8;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: baseline;
}

.stat-unit {
  font-size: 14px;
  margin-left: 4px;
  opacity: 0.7;
}
</style>