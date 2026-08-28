<template>
  <el-container style="height:100vh">
    <el-aside width="200px">
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/products">商品管理</el-menu-item>
        <el-menu-item index="/orders">订单管理</el-menu-item>
        <el-menu-item index="/live">直播管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/trace">溯源维护</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/knowledge">知识库管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center">
        <span>眉山助农电商平台管理后台</span>
        <el-button size="small" @click="onLogout">退出登录（{{ auth.user.nickname || '' }}）</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const isAdmin = computed(() => auth.user.role === 'ADMIN');
const router = useRouter();
function onLogout() { auth.logout(); router.push('/login'); }
</script>
