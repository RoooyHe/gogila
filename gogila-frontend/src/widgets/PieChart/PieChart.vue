<script setup>
import * as echarts from 'echarts';
import { onMounted, ref, watch } from 'vue';

const props = defineProps({
  title: String,
  data: { type: Array, default: () => [] },
  colors: { type: Array, default: () => ['#38bdf8', '#8b5cf6', '#ec4899', '#10b981', '#f59e0b'] }
});

const chartRef = ref(null);
let chart = null;

onMounted(() => {
  chart = echarts.init(chartRef.value);
  updateChart();
});

function updateChart() {
  if (!chart) return;
  chart.setOption({
    title: { text: props.title, textStyle: { color: '#e5e7eb' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { textStyle: { color: '#cbd5e1' }, orient: 'vertical', right: '10%' },
    series: [
      {
        type: 'pie',
        data: props.data,
        itemStyle: { borderRadius: 8 },
        label: { color: '#cbd5e1' }
      }
    ],
    color: props.colors,
    backgroundColor: 'transparent'
  });
}

watch([() => props.data, () => props.title, () => props.colors], () => {
  updateChart();
});
</script>

<template>
  <div ref="chartRef" style="width:100%;height:100%"></div>
</template>
