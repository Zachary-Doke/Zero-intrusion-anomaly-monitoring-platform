<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  entries: {
    type: Array,
    required: true
  }
});
</script>

<template>
  <section class="detail-section">
    <h4>{{ title }}</h4>
    <div class="kv-list">
      <div v-for="[key, value] in entries" :key="key" class="kv-row">
        <span class="kv-key">{{ key }}</span>
        <span class="kv-value">{{ value }}</span>
      </div>
    </div>
  </section>
</template>
