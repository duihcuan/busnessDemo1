<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="(v, k) in stats" :key="k">
        <el-card><div class="num">{{ v }}</div><div>{{ label(k) }}</div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../api/request';

const stats = ref({});
const labels = { orderCount: '订单数', salesAmount: '销售额', productCount: '商品数', liveCount: '直播中', userCount: '用户数' };
const label = (k) => labels[k] || k;

onMounted(async () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  stats.value = user.role === 'ADMIN'
    ? await request.get('/admin/stats')
    : await request.get('/seller/stats');
});
</script>
