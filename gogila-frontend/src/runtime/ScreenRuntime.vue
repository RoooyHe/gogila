<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import http from '../api/http';
import type {ScreenSchema, WidgetConfig} from '../types/screen';
import StatCard from '../widgets/StatCard/StatCard.vue';
import LineChart from '../widgets/LineChart/LineChart.vue';
import BarChart from '../widgets/BarChart/BarChart.vue';
import PieChart from '../widgets/PieChart/PieChart.vue';
import NumberFlop from '../widgets/NumberFlop/NumberFlop.vue';
import ProgressBar from '../widgets/ProgressBar/ProgressBar.vue';
import Table from '../widgets/Table/Table.vue';
import Map from '../widgets/Map/Map.vue';

const route = useRoute();
const screen = ref<ScreenSchema | null>(null);
const loading = ref(false);

const widgetMap: Record<string, any> = {
  'stat-card': StatCard,
  'line-chart': LineChart,
  'bar-chart': BarChart,
  'pie-chart': PieChart,
  'number-flop': NumberFlop,
  'progress-bar': ProgressBar,
  'table': Table,
  'map': Map
};

const backgroundStyle = computed(() => {
  if (!screen.value?.canvasConfig) return {};
  const cfg = screen.value.canvasConfig;
  return {
    width: cfg.width ? `${cfg.width}px` : '100vw',
    height: cfg.height ? `${cfg.height}px` : '100vh',
    backgroundColor: cfg.bgColor || '#020617',
    backgroundImage: cfg.bgImage ? `url(${cfg.bgImage})` : undefined,
    backgroundSize: 'cover',
    position: 'relative',
    overflow: 'hidden'
  };
});

async function fetchScreen() {
  const code = route.params.code as string;
  loading.value = true;
  try {
    const resp = await http.get(`/screens/${code}`);
    screen.value = resp.data as ScreenSchema;
  } catch (e) {
    console.error('加载大屏失败', e);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchScreen();
});

function widgetStyle(widget: WidgetConfig) {
  const {x, y, w, h} = widget.position;
  return {
    position: 'absolute',
    left: x + 'px',
    top: y + 'px',
    width: w + 'px',
    height: h + 'px'
  };
}
</script>

<template>
  <div v-if="loading" class="screen-loading">
    正在加载大屏...
  </div>
  <div v-else-if="screen" class="screen-runtime" :style="backgroundStyle">
    <div
        v-for="w in screen.widgets"
        :key="w.id"
        class="widget-wrapper"
        :style="widgetStyle(w)"
    >
      <component
          :is="widgetMap[w.type]"
          v-bind="w.props"
          :data-binding="w.dataBinding"
      />
    </div>
  </div>
  <div v-else class="screen-empty">
    未找到对应大屏
  </div>
</template>

<style scoped>
.screen-loading,
.screen-empty {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e5e7eb;
  background: #020617;
}

.screen-runtime {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI',
  sans-serif;
}

.widget-wrapper {
  box-sizing: border-box;
}
</style>