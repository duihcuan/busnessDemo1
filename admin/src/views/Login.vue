<template>
  <el-card class="login-card">
    <h2>眉山助农电商平台管理后台</h2>
    <el-form @submit.prevent="onSubmit">
      <el-form-item><el-input v-model="username" placeholder="用户名" /></el-form-item>
      <el-form-item><el-input v-model="password" type="password" placeholder="密码" show-password /></el-form-item>
      <el-button type="primary" style="width:100%" @click="onSubmit">登录</el-button>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const username = ref('admin');
const password = ref('123456');
const router = useRouter();
const auth = useAuthStore();

async function onSubmit() {
  await auth.login(username.value, password.value);
  router.push('/dashboard');
}
</script>

<style scoped>.login-card { width: 360px; margin: 120px auto; }</style>
