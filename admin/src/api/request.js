import axios from 'axios';
import { ElMessage } from 'element-plus';

const request = axios.create({ baseURL: '/api', timeout: 20000 });

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken');
  if (token) config.headers.satoken = token;
  return config;
});

request.interceptors.response.use(
  (res) => {
    const body = res.data;
    if (body.code === 200) return body.data;
    if (body.code === 401) {
      localStorage.removeItem('satoken');
      location.href = '/#/login';
    }
    ElMessage.error(body.message || '请求失败');
    return Promise.reject(new Error(body.message || '请求失败'));
  },
  (err) => {
    ElMessage.error('网络异常：' + (err.message || ''));
    return Promise.reject(err);
  }
);

export default request;
