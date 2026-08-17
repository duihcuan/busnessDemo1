import { createRouter, createWebHashHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Layout from '../views/Layout.vue';

const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'products', component: () => import('../views/Products.vue') },
      { path: 'orders', component: () => import('../views/Orders.vue') },
      { path: 'live', component: () => import('../views/Live.vue') },
      { path: 'trace', component: () => import('../views/Trace.vue') },
      { path: 'knowledge', component: () => import('../views/Knowledge.vue') }
    ]
  }
];

const router = createRouter({ history: createWebHashHistory(), routes });

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('satoken')) return '/login';
});

export default router;
