<script setup>
import { ref, onMounted } from 'vue'
import { pageResources, createResource, updateResource, deleteResource } from '../api/resource'
import { listCategories } from '../api/category'
import { isAdmin } from '../utils/auth'
import { RESOURCE_STATUS, RESOURCE_TYPE } from '../utils/constants'
import { showSuccess, showError } from '../utils/message'

const admin = isAdmin()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const query = ref({ keyword: '', type: '', categoryId: '' })
const categories = ref([])

const showModal = ref(false)
const editingId = ref(null)
const form = ref({ name: '', categoryId: null, type: 'ITEM', description: '', location: '', totalCount: 1 })
const saving = ref(false)

async function load() {
  try {
    const params = { page: page.value, size: size.value }
    if (query.value.keyword) params.keyword = query.value.keyword
    if (query.value.type) params.type = query.value.type
    if (query.value.categoryId) params.categoryId = query.value.categoryId
    const data = await pageResources(params)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // 拦截器已提示错误
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  query.value = { keyword: '', type: '', categoryId: '' }
  search()
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', categoryId: null, type: 'ITEM', description: '', location: '', totalCount: 1 }
  showModal.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    categoryId: row.categoryId,
    type: row.type,
    description: row.description || '',
    location: row.location || '',
    totalCount: row.totalCount
  }
  showModal.value = true
}

async function save() {
  if (!form.value.name) {
    showError('请输入资源名称')
    return
  }
  if (!form.value.categoryId) {
    showError('请选择分类')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      categoryId: Number(form.value.categoryId),
      type: form.value.type,
      description: form.value.description || undefined,
      location: form.value.location || undefined,
      totalCount: form.value.totalCount
    }
    if (editingId.value) {
      await updateResource(editingId.value, payload)
      showSuccess('更新成功')
    } else {
      await createResource(payload)
      showSuccess('创建成功（新资源状态为待审核）')
    }
    showModal.value = false
    load()
  } catch (e) {
    // 拦截器已提示错误
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  if (!confirm(`确认删除「${row.name}」？`)) return
  try {
    await deleteResource(row.id)
    showSuccess('删除成功')
    load()
  } catch (e) {
    // 拦截器已提示错误
  }
}

onMounted(async () => {
  try {
    categories.value = (await listCategories()) || []
  } catch (e) {
    // 拦截器已提示错误
  }
  load()
})
</script>

<template>
  <div>
    <h1>资源列表</h1>

    <div class="toolbar">
      <div class="field">
        <label>关键词</label>
        <input v-model="query.keyword" placeholder="资源名称" style="width:180px" @keyup.enter="search" />
      </div>
      <div class="field">
        <label>类型</label>
        <select v-model="query.type" style="width:120px">
          <option value="">全部</option>
          <option value="ITEM">物品</option>
          <option value="VENUE">场地</option>
        </select>
      </div>
      <div class="field">
        <label>分类</label>
        <select v-model="query.categoryId" style="width:140px">
          <option value="">全部</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <button class="primary" @click="search">查询</button>
      <button @click="reset">重置</button>
      <button v-if="admin" class="primary" @click="openCreate">新增资源</button>
    </div>

    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>类型</th>
          <th>分类</th>
          <th>描述</th>
          <th>状态</th>
          <th>数量</th>
          <th>位置</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in list" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.name }}</td>
          <td>{{ RESOURCE_TYPE[r.type] || r.type }}</td>
          <td>{{ r.categoryName }}</td>
          <td>{{ r.description }}</td>
          <td>
            <span class="tag" :class="r.status === 1 ? 'on' : r.status === 2 ? 'off' : ''">
              {{ RESOURCE_STATUS[r.status] ?? r.status }}
            </span>
          </td>
          <td>{{ r.totalCount }}</td>
          <td>{{ r.location }}</td>
          <td>
            <router-link :to="`/resources/${r.id}`"><button>详情</button></router-link>
            <template v-if="admin">
              <button @click="openEdit(r)">编辑</button>
              <button class="danger" @click="remove(r)">删除</button>
            </template>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="9" style="text-align:center;color:#999">暂无数据</td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页（{{ total }} 条）</span>
      <button :disabled="page >= Math.ceil(total / size)" @click="page++; load()">下一页</button>
    </div>

    <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <div class="modal">
        <h2>{{ editingId ? '编辑资源' : '新增资源' }}</h2>
        <label>名称</label>
        <input v-model="form.name" />
        <label>分类</label>
        <select v-model="form.categoryId">
          <option :value="null" disabled>请选择</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <label>类型</label>
        <select v-model="form.type">
          <option value="ITEM">物品</option>
          <option value="VENUE">场地</option>
        </select>
        <label>数量</label>
        <input v-model.number="form.totalCount" type="number" min="1" />
        <label>位置</label>
        <input v-model="form.location" />
        <label>描述</label>
        <textarea v-model="form.description" rows="3"></textarea>
        <div class="actions">
          <button @click="showModal = false">取消</button>
          <button class="primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
