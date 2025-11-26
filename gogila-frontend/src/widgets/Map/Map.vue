<script setup>
import * as echarts from 'echarts';
import { onMounted, ref, watch } from 'vue';

const props = defineProps({
  title: String,
  center: { type: Array, default: () => [104.065, 37.6952] },
  zoom: { type: Number, default: 4 },
  locations: { type: Array, default: () => [] }
});

const chartRef = ref(null);
let chart = null;

onMounted(() => {
  chart = echarts.init(chartRef.value);
  updateChart();
});

function updateChart() {
  if (!chart) return;

  const mapData = props.locations.map(loc => ({
    name: loc.name,
    value: [...loc.coord, loc.value || 0]
  }));

  chart.setOption({
    title: { text: props.title, textStyle: { color: '#e5e7eb' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    geo: {
      map: 'china',
      center: props.center,
      zoom: props.zoom,
      itemStyle: {
        areaColor: '#1f2937',
        borderColor: '#374151'
      },
      emphasis: {
        itemStyle: {
          areaColor: '#4b5563'
        }
      },
      label: {
        color: '#cbd5e1'
      }
    },
    series: [
      {
        name: '分布',
        type: 'scatter',
        coordinateSystem: 'geo',
        data: mapData,
        symbolSize: function(val) {
          return val[2] / 10;
        },
        itemStyle: {
          color: '#38bdf8'
        },
        label: {
          formatter: '{b}',
          position: 'right',
          color: '#cbd5e1'
        }
      }
    ],
    backgroundColor: 'transparent'
  });
}

watch([() => props.locations, () => props.title, () => props.center, () => props.zoom], () => {
  updateChart();
});
</script>

<template>
  <div ref="chartRef" style="width:100%;height:100%"></div>
</template>
