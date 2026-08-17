const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8');
}

const checks = [
  ['src/stores/auth.js', '/auth/admin-login'],
  ['src/views/Layout.vue', '仪表盘'],
  ['src/views/Layout.vue', '商品管理'],
  ['src/views/Layout.vue', '订单管理'],
  ['src/views/Layout.vue', '直播管理'],
  ['src/views/Layout.vue', '溯源维护'],
  ['src/views/Layout.vue', '知识库管理'],
  ['src/views/Dashboard.vue', '/seller/stats'],
  ['src/views/Dashboard.vue', '/admin/stats'],
  ['src/views/Products.vue', '/products'],
  ['src/views/Products.vue', '/status'],
  ['src/views/Orders.vue', '/seller/orders'],
  ['src/views/Orders.vue', '/ship'],
  ['src/views/Orders.vue', '/refund'],
  ['src/views/Live.vue', '/live/rooms'],
  ['src/views/Live.vue', '/products'],
  ['src/views/Trace.vue', '/trace'],
  ['src/views/Knowledge.vue', '/rag/documents'],
  ['src/views/Knowledge.vue', '/rag/chat'],
  ['src/views/Knowledge.vue', '/rag/messages']
];

const errors = [];
for (const [file, needle] of checks) {
  if (!read(file).includes(needle)) errors.push(file + ' 缺少: ' + needle);
}
if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log('SMOKE_OK');
