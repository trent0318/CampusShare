<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getResource } from '../api/resource'
import { createReservation } from '../api/reservation'
import { RESOURCE_STATUS, RESOURCE_TYPE } from '../utils/constants'
import { showSuccess, showError } from '../utils/message'

const route = useRoute()
const resource = ref(null)
const showReserve = ref(false)
const form = ref({ startTime: '', endTime: '', remark: '' })
const submitting = ref(false)

function fmt(s) {
  return s ? String(s).replace('T', ' ') : ''
}

async function load() {
  try {
    resource.value = await getResource(route.params.id)
  } catch (e) {
    // 拦截器已提示错误
  }
}

function openReserve() {
  form.value = { startTime: '', endTime: '', remark: '' }
  showReserve.value = true
}

async function submitReserve() {
  if (!form.value.startTime || !form.value.endTime) {
    showError('请选择开始和结束时间')
    return
  }
  if (form.value.startTime >= form.value.endTime) {
    showError('开始时间必须早于结束时间')
    return
  }
  submitting.value = true
  try {
    await createReservation({
      resourceId: resource.value.id,
      startTime: form.value.startTime + ':00',
      endTime: form.value.endTime + ':00',
      remark: form.value.remark || undefined
    })
    showSuccess('预约成功')
    showReserve.value = false
  } catch (e) {
    // 拦截器已提示错误
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <router-link to="/resources">← 返回列表</router-link>

    <div v-if="resource" class="card">
      <h1>{{ resource.name }}</h1>
      <div class="detail-grid">
        <span class="label">类型</span><span>{{ RESOURCE_TYPE[resource.type] || resource.type }}</span>
        <span class="label">分类</span><span>{{ resource.categoryName }}</span>
        <span class="label">状态</span>
        <span>
          <span class="tag" :class="resource.status === 1 ? 'on' : ''">
            {{ RESOURCE_STATUS[resource.status] ?? resource.status }}
          </span>
        </span>
        <span class="label">数量</span><span>{{ resource.totalCount }}</span>
        <span class="label">位置</span><span>{{ resource.location || '-' }}</span>
        <span class="label">描述</span><span>{{ resource.description || '-' }}</span>
        <span class="label">创建时间</span><span>{{ fmt(resource.createTime) }}</span>
        <span class="label">更新时间</span><span>{{ fmt(resource.updateTime) }}</span>
      </div>
      <div style="margin-top:16px">
        <button v-if="resource.status === 1" class="primary" @click="openReserve">预约该资源</button>
        <span v-else style="color:#999">该资源当前不可预约</span>
      </div>
    </div>
    <div v-else class="card" style="color:#999">加载中...</div>

    <div v-if="showReserve" class="modal-mask" @click.self="showReserve = false">
      <div class="modal">
        <h2>预约「{{ resource.name }}」</h2>
        <label>开始时间</label>
        <input v-model="form.startTime" type="datetime-local" />
        <label>结束时间</label>
        <input v-model="form.endTime" type="datetime-local" />
        <label>备注（可选）</label>
        <input v-model="form.remark" />
        <div class="actions">
          <button @click="showReserve = false">取消</button>
          <button class="primary" :disabled="submitting" @click="submitReserve">
            {{ submitting ? '提交中...' : '提交预约' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
