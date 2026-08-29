<script setup>
import { ref, onMounted } from 'vue'
import { listMyReservations, cancelReservation, checkinReservation, completeReservation } from '../api/reservation'
import { RESERVATION_STATUS } from '../utils/constants'
import { showSuccess } from '../utils/message'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')

function fmt(s) {
  return s ? String(s).replace('T', ' ') : ''
}

async function load() {
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const data = await listMyReservations(params)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // 拦截器已提示错误
  }
}

function filter() {
  page.value = 1
  load()
}

async function cancel(row) {
  const reason = prompt('取消原因（可留空）')
  if (reason === null) return
  try {
    await cancelReservation(row.id, reason)
    showSuccess('已取消')
    load()
  } catch (e) {
    // 拦截器已提示错误
  }
}

async function checkin(row) {
  if (!confirm('确认签到？')) return
  try {
    await checkinReservation(row.id)
    showSuccess('签到成功')
    load()
  } catch (e) {
    // 拦截器已提示错误
  }
}

async function complete(row) {
  if (!confirm('确认完成该预约？')) return
  try {
    await completeReservation(row.id)
    showSuccess('已完成')
    load()
  } catch (e) {
    // 拦截器已提示错误
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>我的预约</h1>

    <div class="toolbar">
      <div class="field">
        <label>状态</label>
        <select v-model="statusFilter" style="width:140px" @change="filter">
          <option value="">全部</option>
          <option value="CONFIRMED">已确认</option>
          <option value="IN_USE">使用中</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
          <option value="EXPIRED">已过期</option>
        </select>
      </div>
    </div>

    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>资源名称</th>
          <th>预约时间</th>
          <th>状态</th>
          <th>备注</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in list" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.resourceName }}</td>
          <td>{{ fmt(r.startTime) }} ~ {{ fmt(r.endTime) }}</td>
          <td>
            <span class="tag" :class="r.status === 'CONFIRMED' || r.status === 'IN_USE' ? 'on' : ''">
              {{ RESERVATION_STATUS[r.status] || r.status }}
            </span>
          </td>
          <td>{{ r.remark || '-' }}</td>
          <td>
            <template v-if="r.status === 'CONFIRMED'">
              <button class="primary" @click="checkin(r)">签到</button>
              <button class="danger" @click="cancel(r)">取消</button>
            </template>
            <button v-else-if="r.status === 'IN_USE'" class="primary" @click="complete(r)">完成</button>
            <span v-else style="color:#999">-</span>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="6" style="text-align:center;color:#999">暂无预约</td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页（{{ total }} 条）</span>
      <button :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>
