<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  },
  selectedId: {
    type: String,
    default: null
  }
});

const emit = defineEmits(["select", "analyze"]);
</script>

<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>异常类型</th>
          <th>栈顶方法</th>
          <th>次数</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody v-if="items.length > 0">
        <tr
          v-for="item in items"
          :key="item.fingerprint"
          :class="{ 'is-active': item.fingerprint === selectedId }"
          @click="emit('select', item.fingerprint)"
        >
          <td>{{ item.exceptionClass }}</td>
          <td>{{ item.topStackFrame }}</td>
          <td>{{ item.occurrenceCount }}</td>
          <td>
            <button
              class="primary-button"
              type="button"
              @click.stop="emit('analyze', item.fingerprint)"
            >
              查看
            </button>
          </td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <td colspan="4" class="empty-cell">暂无异常指纹</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
