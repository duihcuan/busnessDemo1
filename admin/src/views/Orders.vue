<template>
  <el-table :data="list">
    <el-table-column prop="orderNo" label="订单号" width="200" />
    <el-table-column prop="totalAmount" label="金额" width="100" />
    <el-table-column prop="status" label="状态" width="110" />
    <el-table-column prop="receiverName" label="收件人" width="100" />
    <el-table-column prop="logisticsNo" label="物流单号" width="140" />
    <el-table-column label="操作" width="200">
      <template #default="{ row }">
        <el-button v-if="row.status === 'PAID'" size="small" type="primary" @click="openShip(row)">发货</el-button>
        <el-button v-if="['PAID','SHIPPED','COMPLETED'].includes(row.status)" size="small" type="danger" @click="refund(row)">退款</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-dialog v-model="shipDialog" title="发货" width="420px">
    <el-input v-model="ship.logisticsCompany" placeholder="物流公司" style="margin-bottom:12px" />
    <el-input v-model="ship.logisticsNo" placeholder="物流单号" />
    <template #footer><el-button type="primary" @click="confirmShip">确认发货</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../api/request';

const list = ref([]);
const shipDialog = ref(false);
const ship = ref({});

async function load() {
  const page = await request.get('/seller/orders?page=1&size=100');
  list.value = page.records;
}
function openShip(row) { ship.value = { id: row.id, logisticsCompany: '', logisticsNo: '' }; shipDialog.value = true; }
async function confirmShip() {
  await request.put('/orders/' + ship.value.id + '/ship', ship.value);
  shipDialog.value = false;
  load();
}
async function refund(row) {
  await request.post('/orders/' + row.id + '/refund', {});
  load();
}
onMounted(load);
</script>
