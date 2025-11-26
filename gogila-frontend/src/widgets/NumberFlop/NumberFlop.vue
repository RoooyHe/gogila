<script setup>
const props = defineProps({
  value: Number,
  color: String,
  fontSize: { type: Number, default: 32 }
});
</script>

<template>
  <div :style="{ fontSize: fontSize + 'px', color: color }">
    {{ value }}
  </div>
</template>