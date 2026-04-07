<script setup>
import { computed } from "vue";

const props = defineProps({
  trends: {
    type: Array,
    required: true
  }
});

const maxCount = computed(() => {
  if (props.trends.length === 0) {
    return 1;
  }
  return Math.max(...props.trends.map((item) => item.count), 1);
});
</script>

<template>
  <div class="trend-chart" v-if="trends.length > 0">
    <div v-for="item in trends" :key="item.date" class="trend-bar">
      <span>{{ item.date }}</span>
      <div class="trend-track">
        <div class="trend-fill" :style="{ width: `${(item.count / maxCount) * 100}%` }"></div>
      </div>
      <strong>{{ item.count }}</strong>
    </div>
  </div>
  <div class="trend-chart" v-else>
    <p class="empty-block">最近 7 天暂无趋势数据。</p>
  </div>
</template>
