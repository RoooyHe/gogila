<script setup>
import * as echarts from 'echarts';
import { onMounted, ref, watch } from 'vue';

const props = defineProps({
  title: String,
  color: { type: String, default: '#38bdf8' },
  data: { type: Array, default: () => [] },
  labels: { type: Array, default: () => [] }
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
    xAxis: { type: 'category', data: props.labels, axisLabel: { color: '#cbd5e1' } },
    yAxis: { type: 'value', axisLabel: { color: '#cbd5e1' } },
    series: [
      { type: 'bar', data: props.data, itemStyle: { color: props.color } }
    ],
    backgroundColor: 'transparent',
    grid: { left: '50px', right: '20px', top: '40px', bottom: '30px' }
  });
}

watch([() => props.data, () => props.labels, () => props.title, () => props.color], () => {
  updateChart();
});
</script>

<template>
  <div ref="chartRef" style="width:100%;height:100%"></div>
</template>
