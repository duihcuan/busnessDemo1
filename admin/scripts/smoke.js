const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');

function read(file) {
  return fs.readFileSync(path.join(root, 'src/views', file), 'utf8');
}

const checks = [
  ['Login.vue', '/auth/admin-login'],
  ['Layout.vue', '仪表盘'],
  ['Layout.vue', '商品管理'],
  ['Layout.vue', '订单管理'],
  ['Layout.vue', '直播管理'],
  ['Layout.vue', '溯源维护'],
  ['Layout.vue', '知识库管理'],
  ['Dashboard.vue', '/seller/stats'],
  ['Dashboard.vue', '/admin/stats'],
  ['Products.vue', '/products'],
  ['Products.vue', '/status'],
  ['Orders.vue', '/seller/orders'],
  ['Orders.vue', '/ship'],
  ['Orders.vue', '/refund'],
  ['Live.vue', '/live/rooms'],
  ['Live.vue', '/products'],
  ['Trace.vue', '/trace'],
  ['Knowledge.vue', '/rag/documents'],
  ['Knowledge.vue', '/rag/chat'],
  ['Knowledge.vue', '/rag/messages']
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
