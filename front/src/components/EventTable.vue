<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  },
  selectedId: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(["select"]);

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").replace(/\.\d+$/, "");
}

function severityClass(severity) {
  if (severity === "CRITICAL") {
    return "severity-critical";
  }
  if (severity === "HIGH") {
    return "severity-high";
  }
  return "severity-medium";
}

function alertClass(status) {
  if (status === "TRIGGERED") {
    return "alert-triggered";
  }
  if (status === "PENDING") {
    return "alert-pending";
  }
  return "alert-observed";
}
</script>

<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>时间</th>
          <th>服务</th>
          <th>异常类型</th>
          <th>级别</th>
          <th>状态</th>
          <th>告警</th>
          <th>摘要</th>
        </tr>
      </thead>
      <tbody v-if="items.length > 0">
        <tr
          v-for="item in items"
          :key="item.id"
          :class="{ 'is-active': item.id === selectedId }"
          @click="emit('select', item.id)"
        >
          <td>{{ formatDateTime(item.occurrenceTime) }}</td>
          <td>{{ item.serviceName || item.appName || "-" }}</td>
          <td>{{ item.exceptionClass }}</td>
          <td>
            <span class="severity-chip" :class="severityClass(item.severity)">
              {{ item.severity || "-" }}
            </span>
          </td>
          <td>
            <span class="status-chip" :class="`status-${(item.status || 'OPEN').toLowerCase()}`">
              {{ item.status || "OPEN" }}
            </span>
          </td>
          <td>
            <span class="alert-chip" :class="alertClass(item.alertStatus)">
              {{ item.alertStatus || "-" }}
            </span>
          </td>
          <td>{{ item.summary || item.methodName || "-" }}</td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr>
          <td colspan="7" class="empty-cell">暂无异常事件</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
