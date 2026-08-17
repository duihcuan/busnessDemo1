import { defineStore } from 'pinia';
import request from '../api/request';

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('satoken') || '', user: JSON.parse(localStorage.getItem('user') || '{}') }),
  actions: {
    async login(username, password) {
      const data = await request.post('/auth/admin-login', { username, password });
      this.token = data.token;
      this.user = data;
      localStorage.setItem('satoken', data.token);
      localStorage.setItem('user', JSON.stringify(data));
    },
    logout() {
      this.token = '';
      this.user = {};
      localStorage.removeItem('satoken');
      localStorage.removeItem('user');
    }
  }
});
