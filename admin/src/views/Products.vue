<template>
  <div>
    <el-button type="primary" @click="openForm()">新增商品</el-button>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="price" label="价格" width="90" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="origin" label="产地" width="90" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openForm(row)">编辑</el-button>
          <el-button size="small" @click="toggle(row)">{{ row.status === 'ON_SALE' ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="商品编辑" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类"><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.specText" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :precision="2" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="产地"><el-input v-model="form.origin" /></el-form-item>
        <el-form-item label="溯源编号"><el-input v-model="form.traceCode" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../api/request';

const list = ref([]);
const dialog = ref(false);
const form = ref({});

async function load() {
  const page = await request.get('/products?page=1&size=100');
  list.value = page.records;
}
function openForm(row) { form.value = row ? { ...row } : {}; dialog.value = true; }
async function save() {
  if (form.value.id) await request.put('/products/' + form.value.id, form.value);
  else await request.post('/products', form.value);
  dialog.value = false;
  load();
}
async function toggle(row) {
  await request.put('/products/' + row.id + '/status', { status: row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE' });
  load();
}
async function remove(row) {
  await request.delete('/products/' + row.id);
  ElMessage.success('已删除');
  load();
}
onMounted(load);
</script>
