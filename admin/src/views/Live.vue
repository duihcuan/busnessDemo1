<template>
  <div>
    <el-button type="primary" @click="openCreate">新建直播间</el-button>
    <el-table :data="rooms" style="margin-top:12px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="videoUrl" label="视频地址" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="setStatus(row, 'LIVE')">开播</el-button>
          <el-button size="small" @click="setStatus(row, 'OFFLINE')">下播</el-button>
          <el-button size="small" @click="openProducts(row)">挂商品</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialog" title="新建直播间" width="460px">
      <el-input v-model="createForm.title" placeholder="标题" style="margin-bottom:12px" />
      <el-input v-model="createForm.videoUrl" placeholder="预录视频地址" style="margin-bottom:12px" />
      <el-input v-model="createForm.coverUrl" placeholder="封面地址" />
      <template #footer><el-button type="primary" @click="createRoom">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="productDialog" title="挂载直播商品" width="520px">
      <div v-for="(p, i) in products" :key="i" style="display:flex; gap:8px; margin-bottom:8px;">
        <el-select v-model="p.productId" placeholder="选择商品" filterable style="width:220px">
          <el-option v-for="prod in productOptions" :key="prod.id" :label="prodLabel(prod)" :value="prod.id" />
        </el-select>
        <el-input-number v-model="p.livePrice" :precision="2" :min="0" placeholder="直播价" />
        <el-input-number v-model="p.sort" :min="0" placeholder="排序" />
        <el-button @click="products.splice(i, 1)">删</el-button>
      </div>
      <el-button @click="products.push({ productId: 1, livePrice: 0, sort: products.length })">加一行</el-button>
      <template #footer><el-button type="primary" @click="saveProducts">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../api/request';

const rooms = ref([]);
const createDialog = ref(false);
const createForm = ref({});
const productDialog = ref(false);
const products = ref([]);
const productOptions = ref([]);
let currentRoom = 0;

async function load() {
  rooms.value = await request.get('/live/rooms');
}
function openCreate() { createForm.value = {}; createDialog.value = true; }
async function createRoom() {
  await request.post('/live/rooms', { room: createForm.value });
  createDialog.value = false;
  load();
}
async function setStatus(row, status) {
  await request.put('/live/rooms/' + row.id + '/status', { status });
  load();
}
function prodLabel(prod) { return (prod.name || '') + ' (ID:' + prod.id + ')'; }

function openProducts(row) {
  currentRoom = row.id;
  products.value = [{ productId: null, livePrice: 0, sort: 0 }];
  productDialog.value = true;
}
async function saveProducts() {
  await request.put('/live/rooms/' + currentRoom + '/products', products.value);
  productDialog.value = false;
}
onMounted(async () => { await load(); productOptions.value = (await request.get('/products?page=1&size=100')).records; });
</script>
