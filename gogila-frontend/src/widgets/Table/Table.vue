<script setup>
import { computed, ref } from 'vue';

const props = defineProps({
  title: String,
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  pageSize: { type: Number, default: 10 }
});

const currentPage = ref(1);

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize;
  const end = start + props.pageSize;
  return props.data.slice(start, end);
});

const totalPages = computed(() => {
  return Math.ceil(props.data.length / props.pageSize);
});

function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
}
</script>

<template>
  <div class="table-container">
    <div v-if="title" class="table-title">{{ title }}</div>
    <div class="table-wrapper">
      <table v-if="columns.length && data.length" class="data-table">
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.key">{{ col.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in paginatedData" :key="idx">
            <td v-for="col in columns" :key="col.key">
              {{ row[col.key] ?? '-' }}
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无数据</div>
    </div>
    <div v-if="totalPages > 1" class="table-pagination">
      <button
        class="pagination-btn"
        :disabled="currentPage === 1"
        @click="goToPage(currentPage - 1)"
      >
        上一页
      </button>
      <span class="pagination-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="pagination-btn"
        :disabled="currentPage === totalPages"
        @click="goToPage(currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.table-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #e5e7eb;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.table-wrapper {
  flex: 1;
  overflow: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  background: rgba(51, 65, 85, 0.4);
  position: sticky;
  top: 0;
}

.data-table th {
  padding: 8px 12px;
  text-align: left;
  color: #cbd5e1;
  font-weight: 600;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.data-table td {
  padding: 8px 12px;
  color: #e5e7eb;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.data-table tbody tr:hover {
  background: rgba(51, 65, 85, 0.2);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #94a3b8;
  font-size: 14px;
}

.table-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  margin-top: auto;
}

.pagination-btn {
  padding: 4px 12px;
  background: rgba(51, 65, 85, 0.6);
  color: #cbd5e1;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: rgba(51, 65, 85, 0.8);
  color: #e5e7eb;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  color: #94a3b8;
  font-size: 12px;
}
</style>
