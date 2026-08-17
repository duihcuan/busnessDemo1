<template>
  <div>
    <el-button type="primary" @click="openForm()">新增记录</el-button>
    <el-table :data="list" style="margin-top:12px">
      <el-table-column prop="traceCode" label="溯源编号" width="130" />
      <el-table-column prop="stage" label="阶段" width="100" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="recordDate" label="日期" width="170" />
      <el-table-column prop="operator" label="操作方" width="130" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button size="small" @click="openForm(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="溯源记录" width="480px">
      <el-form label-width="80px">
        <el-form-item label="编号"><el-input v-model="form.traceCode" /></el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.stage">
            <el-option label="种植" value="PLANT" />
            <el-option label="加工" value="PROCESS" />
            <el-option label="质检" value="QC" />
            <el-option label="物流" value="LOGISTICS" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" /></el-form-item>
        <el-form-item label="操作方"><el-input v-model="form.operator" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../api/request';

const list = ref([]);
const dialog = ref(false);
const form = ref({});

async function load() { list.value = await request.get('/trace'); }
function openForm(row) { form.value = row ? { ...row } : {}; dialog.value = true; }
async function save() {
  if (form.value.id) await request.put('/trace/' + form.value.id, form.value);
  else await request.post('/trace', form.value);
  dialog.value = false;
  load();
}
async function remove(row) { await request.delete('/trace/' + row.id); load(); }
onMounted(load);
</script>
