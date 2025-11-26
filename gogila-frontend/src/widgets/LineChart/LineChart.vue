<script setup>
import * as echarts from 'echarts';
import { onMounted, ref } from 'vue';

const props = defineProps({
  title: String,
  color: { type: String, default: '#38bdf8' },
  data: { type: Array, default: () => [] },
  labels: { type: Array, default: () => [] }
});

const chartRef = ref(null);

onMounted(() => {
  const chart = echarts.init(chartRef.value);
  chart.setOption({
    title: { text: props.title, textStyle: { color: '#e5e7eb' } },
    xAxis: { type: 'category', data: props.labels, axisLabel: { color: '#cbd5e1' } },
    yAxis: { type: 'value', axisLabel: { color: '#cbd5e1' } },
    series: [
      { type: 'line', data: props.data, smooth: true, lineStyle: { color: props.color } }
    ],
    backgroundColor: 'transparent'
  });
});
</script>

<template>
  <div ref="chartRef" style="width:100%;height:100%"></div>
</template>