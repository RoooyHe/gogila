<script setup>
import { computed } from 'vue';

const props = defineProps({
  title: String,
  percentage: { type: Number, default: 0 },
  color: { type: String, default: '#38bdf8' },
  showText: { type: Boolean, default: true }
});

const safePercentage = computed(() => {
  const val = Math.max(0, Math.min(100, props.percentage));
  return val;
});
</script>

<template>
  <div class="progress-container">
    <div v-if="title" class="progress-title">{{ title }}</div>
    <div class="progress-bar-wrapper">
      <div class="progress-bar-bg">
        <div
          class="progress-bar-fill"
          :style="{
            width: safePercentage + '%',
            backgroundColor: color
          }"
        ></div>
      </div>
      <div v-if="showText" class="progress-text">{{ safePercentage }}%</div>
    </div>
  </div>
</template>

<style scoped>
.progress-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 12px 16px;
  box-sizing: border-box;
  background: rgba(15, 23, 42, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.progress-title {
  font-size: 14px;
  color: #cbd5e1;
  margin-bottom: 8px;
  opacity: 0.8;
}

.progress-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-bg {
  flex: 1;
  height: 12px;
  background: rgba(51, 65, 85, 0.6);
  border-radius: 6px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 6px;
}

.progress-text {
  font-size: 14px;
  color: #e5e7eb;
  font-weight: 600;
  min-width: 40px;
  text-align: right;
}
</style>
