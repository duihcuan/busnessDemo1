# 微信小程序端实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 交付消费者端微信小程序（原生 WXML/WXSS/JS）：登录（微信一键登录，可降级模拟）、首页/分类/商品详情/溯源、购物车/地址、下单支付模拟/订单、直播列表/直播间（模拟直播）、RAG 客服聊天。

**架构：** `miniprogram/` 独立目录；`utils/request.js` 统一封装 wx.request（baseUrl、satoken 头、401 跳登录）；页面按 spec 7.1 逐页实现；后端接口全部沿用已实现 REST API。

**技术栈：** 微信原生小程序（无第三方框架）、JavaScript（ES6）、微信开发者工具验证；`scripts/check.js`（Node）做页面清单与 JSON 合法性自动校验。

---

## 文件结构

```text
miniprogram/
  app.js                                  # 启动：读 token、跳登录
  app.json                                # 页面注册 + tabBar（首页/分类/购物车/我的）
  app.wxss                                # 全局样式与工具类
  config.js                               # baseUrl = http://127.0.0.1:8080
  utils/request.js                        # Promise 化 wx.request + token + 401 处理
  utils/auth.js                           # 微信登录（降级模拟）、token 存取
  pages/login/login.js/.wxml/.json        # 登录页
  pages/index/index.js/.wxml/.json        # 首页（轮播/分类入口/热销/直播入口）
  pages/category/category.js/.wxml/.json  # 分类与商品列表（筛选）
  pages/detail/detail.js/.wxml/.json      # 商品详情（加购/购买/溯源入口）
  pages/trace/trace.js/.wxml/.json        # 溯源查询
  pages/cart/cart.js/.wxml/.json          # 购物车
  pages/address/address.js/.wxml/.json    # 地址管理（列表+表单）
  pages/confirm/confirm.js/.wxml/.json    # 确认订单（地址/商品/模拟支付）
  pages/orders/orders.js/.wxml/.json      # 订单列表（状态操作/物流）
  pages/live/live.js/.wxml/.json          # 直播列表
  pages/liveroom/liveroom.js/.wxml/.json  # 直播间（视频/弹幕/商品）
  pages/chat/chat.js/.wxml/.json          # RAG 客服（来源+反馈）
  pages/me/me.js/.wxml/.json              # 我的（入口/退出）
  scripts/check.js                        # Node 校验：app.json 页面文件齐全 + JSON 合法
```

页面 `.wxss` 不单独建，统一使用 `app.wxss`（页面级样式文件可缺省）。所有页面通过 `utils/request.js` 访问后端，`Authorization` 头统一为 `satoken`。

---

### 任务 1：项目骨架、公共层与校验脚本

**文件：**
- 创建：`miniprogram/app.js`、`app.json`、`app.wxss`、`config.js`
- 创建：`miniprogram/utils/request.js`、`utils/auth.js`
- 创建：`miniprogram/scripts/check.js`
- 创建：`miniprogram/pages/index/index.js/.wxml/.json`（占位页面，其余任务补全）

- [ ] **步骤 1：编写失败的校验脚本**

```javascript
// miniprogram/scripts/check.js —— 运行：node scripts/check.js
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

const appJson = readJson(path.join(root, 'app.json'));
const errors = [];
for (const page of appJson.pages) {
  for (const ext of ['.js', '.wxml']) {
    const file = path.join(root, page + ext);
    if (!fs.existsSync(file)) errors.push(`缺少页面文件: ${page + ext}`);
  }
}
for (const item of appJson.tabBar.list) {
  if (!appJson.pages.includes(item.pagePath)) errors.push(`tabBar 页面未注册: ${item.pagePath}`);
}
if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log('CHECK_OK: 所有页面文件齐全，app.json 合法');
```

- [ ] **步骤 2：运行脚本验证失败**

运行：`cd miniprogram && node scripts/check.js`
预期：FAIL（app.json 不存在或页面文件缺失）

- [ ] **步骤 3：实现骨架与公共层**

`app.json`：

```json
{
  "pages": [
    "pages/index/index",
    "pages/category/category",
    "pages/cart/cart",
    "pages/me/me",
    "pages/login/login",
    "pages/detail/detail",
    "pages/trace/trace",
    "pages/address/address",
    "pages/confirm/confirm",
    "pages/orders/orders",
    "pages/live/live",
    "pages/liveroom/liveroom",
    "pages/chat/chat"
  ],
  "window": {
    "navigationBarTitleText": "眉山助农商城",
    "navigationBarBackgroundColor": "#1e8e3e",
    "navigationBarTextStyle": "white"
  },
  "tabBar": {
    "color": "#666666",
    "selectedColor": "#1e8e3e",
    "list": [
      { "pagePath": "pages/index/index", "text": "首页" },
      { "pagePath": "pages/category/category", "text": "分类" },
      { "pagePath": "pages/cart/cart", "text": "购物车" },
      { "pagePath": "pages/me/me", "text": "我的" }
    ]
  },
  "style": "v2"
}
```

`config.js`：

```javascript
module.exports = {
  baseUrl: 'http://127.0.0.1:8080'
};
```

`utils/request.js`：

```javascript
const { baseUrl } = require('../config');

function request(path, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        satoken: wx.getStorageSync('satoken') || ''
      },
      success(res) {
        const body = res.data || {};
        if (body.code === 200) {
          resolve(body.data);
        } else if (body.code === 401) {
          wx.removeStorageSync('satoken');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(new Error(body.message || '未登录'));
        } else {
          reject(new Error(body.message || '请求失败'));
        }
      },
      fail(err) {
        reject(new Error('网络异常：' + (err.errMsg || '')));
      }
    });
  });
}

module.exports = {
  get: (p) => request(p),
  post: (p, d) => request(p, 'POST', d),
  put: (p, d) => request(p, 'PUT', d),
  del: (p) => request(p, 'DELETE')
};
```

`utils/auth.js`：

```javascript
const { post } = require('./request');

function saveToken(res) {
  wx.setStorageSync('satoken', res.token);
  wx.setStorageSync('user', res);
}

function wechatLogin(phone) {
  return new Promise((resolve, reject) => {
    wx.login({
      success({ code }) {
        post('/api/auth/wechat-login', { code, phone })
          .then((res) => { saveToken(res); resolve(res); })
          .catch(reject);
      },
      fail: reject
    });
  });
}

function mockLogin(phone, code) {
  return post('/api/auth/mock-login', { phone, code })
    .then((res) => { saveToken(res); return res; });
}

function logout() {
  wx.removeStorageSync('satoken');
  wx.removeStorageSync('user');
}

module.exports = { wechatLogin, mockLogin, logout };
```

`app.wxss`（工具类，全局可用）：

```css
page { background: #f5f6f7; font-size: 28rpx; color: #333; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin: 20rpx; }
.btn-primary { background: #1e8e3e; color: #fff; border-radius: 40rpx; }
.price { color: #e64340; font-weight: bold; }
.tag { display: inline-block; background: #e8f5e9; color: #1e8e3e; border-radius: 6rpx; padding: 4rpx 12rpx; font-size: 22rpx; }
```

`app.js`：

```javascript
App({
  onLaunch() {
    if (!wx.getStorageSync('satoken')) {
      wx.reLaunch({ url: '/pages/login/login' });
    }
  }
});
```

`pages/index/index` 占位（.wxml 显示标题，.js 空 onLoad，.json `{"navigationBarTitleText":"首页"}`）。

- [ ] **步骤 4：运行校验脚本验证通过**

运行：`cd miniprogram && node scripts/check.js`
预期：CHECK_OK（此时除 index 外其余页面需先建占位四文件？——校验仅要求 .js/.wxml，因此其余页面需在任务 2-8 逐个补齐；本任务先注册的页面会报缺文件，属预期，逐步消除）

> 说明：为让校验脚本从任务 1 起即可运行，本任务同时为 app.json 中其余页面创建占位 `.js/.wxml/.json`（内容为最小骨架），后续任务逐个替换为真实实现。

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 小程序骨架与请求/认证公共层"
```

---

### 任务 2：登录页与我的页

**文件：**
- 修改：`miniprogram/pages/login/login.js/.wxml/.json`
- 修改：`miniprogram/pages/me/me.js/.wxml/.json`

- [ ] **步骤 1：编写失败的测试（页面 JS 单测逻辑用 Node 冒烟）**

创建 `miniprogram/scripts/smoke-login.js`：

```javascript
// 校验 auth 工具暴露的接口签名（不依赖微信运行时）
const auth = require('../utils/auth');
if (typeof auth.wechatLogin !== 'function' || typeof auth.mockLogin !== 'function' || typeof auth.logout !== 'function') {
  console.error('auth 工具接口缺失');
  process.exit(1);
}
console.log('SMOKE_OK');
```

运行：`cd miniprogram && node scripts/smoke-login.js`
预期：FAIL（auth.js 尚不存在或未导出）

- [ ] **步骤 2：运行确认失败**

预期：FAIL

- [ ] **步骤 3：实现登录页与我的页**

`pages/login/login.wxml`：

```xml
<view class="card">
  <view class="title">眉山助农商城</view>
  <button class="btn-primary" bindtap="onWechatLogin">微信一键登录</button>
  <input class="input" placeholder="手机号（模拟登录）" value="{{phone}}" bindinput="onPhone" />
  <button bindtap="onMockLogin">模拟登录</button>
</view>
```

`pages/login/login.js`：

```javascript
const { wechatLogin, mockLogin } = require('../../utils/auth');

Page({
  data: { phone: '13900000001' },
  onPhone(e) { this.setData({ phone: e.detail.value }); },
  onWechatLogin() {
    wx.showLoading({ title: '登录中' });
    wechatLogin(this.data.phone)
      .then(() => { wx.hideLoading(); wx.switchTab({ url: '/pages/index/index' }); })
      .catch((err) => { wx.hideLoading(); wx.showToast({ title: err.message, icon: 'none' }); });
  },
  onMockLogin() {
    mockLogin(this.data.phone, '123456')
      .then(() => wx.switchTab({ url: '/pages/index/index' }))
      .catch((err) => wx.showToast({ title: err.message, icon: 'none' }));
  }
});
```

`pages/login/login.json`：`{"navigationBarTitleText":"登录"}`

`pages/me/me.js`：

```javascript
const { logout } = require('../../utils/auth');

Page({
  data: { user: {} },
  onShow() { this.setData({ user: wx.getStorageSync('user') || {} }); },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }); },
  goAddress() { wx.navigateTo({ url: '/pages/address/address' }); },
  goChat() { wx.navigateTo({ url: '/pages/chat/chat' }); },
  onLogout() { logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});
```

`pages/me/me.wxml`：用户昵称 + 我的订单/收货地址/在线客服入口 + 退出登录按钮。

- [ ] **步骤 4：运行冒烟验证**

运行：`cd miniprogram && node scripts/smoke-login.js && node scripts/check.js`
预期：SMOKE_OK + CHECK_OK

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 登录页与我的页"
```

---

### 任务 3：首页与分类商品列表

**文件：**
- 修改：`miniprogram/pages/index/index.js/.wxml/.json`
- 修改：`miniprogram/pages/category/category.js/.wxml/.json`

- [ ] **步骤 1：编写冒烟脚本 `scripts/smoke-pages.js`**

```javascript
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
// 校验首页与分类页 JS 存在且定义了 Page() 调用
for (const page of ['pages/index/index.js', 'pages/category/category.js']) {
  const code = fs.readFileSync(path.join(root, page), 'utf8');
  if (!code.includes('Page({')) { console.error(page + ' 缺少 Page 定义'); process.exit(1); }
}
console.log('PAGES_OK');
```

- [ ] **步骤 2：运行确认失败**

预期：FAIL（占位页无真实逻辑或 Page 定义格式不符）

- [ ] **步骤 3：实现首页与分类页**

`pages/index/index.js`：

```javascript
const { get } = require('../../utils/request');

Page({
  data: { categories: [], products: [] },
  onShow() {
    get('/api/categories').then((categories) => this.setData({ categories }));
    get('/api/products?page=1&size=6').then((page) => this.setData({ products: page.records }));
  },
  goCategory(e) { wx.switchTab({ url: '/pages/category/category' }); },
  goLive() { wx.navigateTo({ url: '/pages/live/live' }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
```

`pages/index/index.wxml`：

```xml
<view class="card">
  <view class="section-title">直播助农</view>
  <button class="btn-primary" bindtap="goLive">进入直播间</button>
</view>
<view class="card">
  <view class="section-title">分类</view>
  <view class="tag" wx:for="{{categories}}" wx:key="id" bindtap="goCategory">{{item.name}}</view>
</view>
<view class="card" wx:for="{{products}}" wx:key="id" data-id="{{item.id}}" bindtap="goDetail">
  <view>{{item.name}}</view>
  <view class="price">¥{{item.price}}</view>
  <view class="tag">{{item.specText}}</view>
</view>
```

`pages/category/category.js`：

```javascript
const { get } = require('../../utils/request');

Page({
  data: { categories: [], current: 0, products: [], keyword: '', total: 0 },
  onShow() {
    get('/api/categories').then((categories) => {
      this.setData({ categories });
      this.load(0);
    });
  },
  load(categoryId) {
    const q = categoryId ? '&categoryId=' + categoryId : '';
    const kw = this.data.keyword ? '&keyword=' + encodeURIComponent(this.data.keyword) : '';
    get('/api/products?page=1&size=20' + q + kw).then((page) =>
      this.setData({ products: page.records, total: page.total }));
  },
  onTab(e) { const i = e.currentTarget.dataset.i; this.setData({ current: i }); this.load(this.data.categories[i].id); },
  onKeyword(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.load(this.data.categories[this.data.current].id); },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
```

`pages/category/category.wxml`：横向分类 tab + 搜索框 + 商品卡片列表（结构与首页商品卡一致）。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-pages.js && node scripts/check.js`
预期：PAGES_OK + CHECK_OK

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 首页与分类商品列表"
```

---

### 任务 4：商品详情与溯源

**文件：**
- 修改：`miniprogram/pages/detail/detail.js/.wxml/.json`
- 修改：`miniprogram/pages/trace/trace.js/.wxml/.json`

- [ ] **步骤 1：冒烟脚本 `scripts/smoke-detail.js`**（校验两页 JS 含关键方法名：onAddCart/onBuy/goTrace 与 query）

- [ ] **步骤 2：运行确认失败**

- [ ] **步骤 3：实现**

`pages/detail/detail.js`：

```javascript
const { get, post } = require('../../utils/request');

Page({
  data: { id: 0, product: {}, qty: 1 },
  onLoad(options) {
    this.setData({ id: Number(options.id) });
    get('/api/products/' + options.id).then((product) => this.setData({ product }));
  },
  onQty(e) { this.setData({ qty: Math.max(1, Number(e.detail.value)) }); },
  onAddCart() {
    post('/api/cart', { productId: this.data.id, quantity: this.data.qty })
      .then(() => wx.showToast({ title: '已加入购物车' }));
  },
  onBuy() {
    wx.navigateTo({ url: '/pages/confirm/confirm?productId=' + this.data.id + '&qty=' + this.data.qty });
  },
  goTrace() { wx.navigateTo({ url: '/pages/trace/trace?code=' + (this.data.product.traceCode || '') }); }
});
```

`pages/detail/detail.wxml`：商品图（mainImage 为空显示占位）、名称/规格/价格/库存、产地与溯源编号（"查看溯源"按钮）、数量输入、底部"加入购物车 / 立即购买"。

`pages/trace/trace.js`：

```javascript
const { get } = require('../../utils/request');

Page({
  data: { code: '', records: [] },
  onLoad(options) {
    if (options.code) this.query(options.code);
  },
  onCode(e) { this.setData({ code: e.detail.value }); },
  query(code) {
    const c = code || this.data.code;
    if (!c) return wx.showToast({ title: '请输入溯源编号', icon: 'none' });
    get('/api/trace/' + encodeURIComponent(c)).then((records) => this.setData({ records }));
  }
});
```

`pages/trace/trace.wxml`：输入框 + 查询按钮 + 按时间线展示四阶段记录（stage 显示为 种植/加工/质检/物流）。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-detail.js && node scripts/check.js`
预期：通过

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 商品详情与溯源查询"
```

---

### 任务 5：购物车与地址管理

**文件：**
- 修改：`miniprogram/pages/cart/cart.js/.wxml/.json`
- 修改：`miniprogram/pages/address/address.js/.wxml/.json`

- [ ] **步骤 1：冒烟脚本 `scripts/smoke-cart.js`**（校验 cart 含 loadCart/onQty/onDelete/goCheckout，address 含 load/onSave/onSetDefault/onDelete）

- [ ] **步骤 2：运行确认失败**

- [ ] **步骤 3：实现**

`pages/cart/cart.js`：

```javascript
const { get, post, put, del } = require('../../utils/request');

Page({
  data: { items: [] },
  onShow() { this.loadCart(); },
  loadCart() { get('/api/cart').then((items) => this.setData({ items })); },
  onQty(e) {
    const id = e.currentTarget.dataset.id;
    const qty = Number(e.detail.value);
    put('/api/cart/' + id, { quantity: qty }).then(() => this.loadCart());
  },
  onDelete(e) { del('/api/cart/' + e.currentTarget.dataset.id).then(() => this.loadCart()); },
  goCheckout() {
    if (!this.data.items.length) return wx.showToast({ title: '购物车为空', icon: 'none' });
    wx.navigateTo({ url: '/pages/confirm/confirm?fromCart=1' });
  }
});
```

`pages/cart/cart.wxml`：商品行（名称/规格/价格/数量 stepper/删除）+ 底部"去结算"。

`pages/address/address.js`：

```javascript
const { get, post, put, del } = require('../../utils/request');

Page({
  data: { list: [], form: {}, editing: false, showForm: false },
  onShow() { this.load(); },
  load() { get('/api/addresses').then((list) => this.setData({ list })); },
  openForm(e) {
    const item = e.currentTarget.dataset.item || {};
    this.setData({ showForm: true, editing: !!item.id, form: item });
  },
  onField(e) { this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value }); },
  onSave() {
    const f = this.data.form;
    const req = this.data.editing ? put('/api/addresses/' + f.id, f) : post('/api/addresses', f);
    req.then(() => { this.setData({ showForm: false }); this.load(); });
  },
  onSetDefault(e) {
    const item = this.data.list.find((x) => x.id === e.currentTarget.dataset.id);
    put('/api/addresses/' + item.id, { ...item, isDefault: 1 }).then(() => this.load());
  },
  onDelete(e) { del('/api/addresses/' + e.currentTarget.dataset.id).then(() => this.load()); }
});
```

`pages/address/address.wxml`：地址列表（默认标记/设默认/删除）+ 新增按钮 + 表单弹层（收件人/电话/省市区/详细地址/设为默认）。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-cart.js && node scripts/check.js`
预期：通过

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 购物车与地址管理"
```

---

### 任务 6：确认订单与订单列表

**文件：**
- 修改：`miniprogram/pages/confirm/confirm.js/.wxml/.json`
- 修改：`miniprogram/pages/orders/orders.js/.wxml/.json`

- [ ] **步骤 1：冒烟脚本 `scripts/smoke-order.js`**（校验 confirm 含 loadAddress/onSubmit/onPay，orders 含 load/action）

- [ ] **步骤 2：运行确认失败**

- [ ] **步骤 3：实现**

`pages/confirm/confirm.js`：

```javascript
const { get, post } = require('../../utils/request');

Page({
  data: { productId: 0, qty: 1, product: null, cartItems: [], address: null, addresses: [] },
  onLoad(options) {
    if (options.productId) {
      this.setData({ productId: Number(options.productId), qty: Number(options.qty || 1) });
      get('/api/products/' + options.productId).then((product) => this.setData({ product }));
    } else {
      get('/api/cart').then((items) => this.setData({ cartItems: items }));
    }
    get('/api/addresses').then((addresses) => {
      const def = addresses.find((a) => a.isDefault === 1) || addresses[0];
      this.setData({ addresses, address: def });
    });
  },
  pickAddress() {
    const items = this.data.addresses.map((a) => a.receiver + ' ' + a.phone + ' ' + a.province + a.city + a.district + a.detail);
    wx.showActionSheet({ itemList: items, success: (res) => this.setData({ address: this.data.addresses[res.tapIndex] }) });
  },
  onSubmit() {
    const addressId = this.data.address ? this.data.address.id : 1;
    const items = this.data.product
      ? [{ productId: this.data.productId, quantity: this.data.qty }]
      : this.data.cartItems.map((c) => ({ productId: c.productId, quantity: c.quantity }));
    post('/api/orders', { addressId, items })
      .then((order) => {
        wx.showModal({
          title: '模拟支付', content: '确认支付 ¥' + order.totalAmount + '？',
          success: (r) => {
            if (r.confirm) {
              post('/api/orders/' + order.id + '/pay', { channel: 'MOCK_WECHAT' })
                .then(() => wx.redirectTo({ url: '/pages/orders/orders' }));
            }
          }
        });
      })
      .catch((err) => wx.showToast({ title: err.message, icon: 'none' }));
  }
});
```

`pages/confirm/confirm.wxml`：地址卡片（点击选择）、商品清单、合计、提交订单按钮。

`pages/orders/orders.js`：

```javascript
const { get, post } = require('../../utils/request');

const STATUS = { PENDING_PAY: '待支付', PAID: '已支付待发货', SHIPPED: '已发货', COMPLETED: '已完成', REFUNDED: '已退款', CANCELLED: '已取消' };

Page({
  data: { orders: [], statusMap: STATUS },
  onShow() { this.load(); },
  load() { get('/api/orders?page=1&size=20').then((page) => this.setData({ orders: page.records })); },
  action(e) {
    const { id, act } = e.currentTarget.dataset;
    const url = '/api/orders/' + id + '/' + act;
    post(url, act === 'pay' ? { channel: 'MOCK_WECHAT' } : {}).then(() => this.load());
  }
});
```

`pages/orders/orders.wxml`：订单卡片（订单号/状态/金额/物流信息），按状态显示操作按钮：待支付→支付/取消；已支付→申请退款；已发货→确认收货；已完成→申请退款。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-order.js && node scripts/check.js`
预期：通过

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 确认订单与订单列表"
```

---

### 任务 7：直播列表与直播间

**文件：**
- 修改：`miniprogram/pages/live/live.js/.wxml/.json`
- 修改：`miniprogram/pages/liveroom/liveroom.js/.wxml/.json`

- [ ] **步骤 1：冒烟脚本 `scripts/smoke-live.js`**（校验 live 含 load/goRoom，liveroom 含 load/sendDanmaku/buy）

- [ ] **步骤 2：运行确认失败**

- [ ] **步骤 3：实现**

`pages/live/live.js`：

```javascript
const { get } = require('../../utils/request');

Page({
  data: { rooms: [] },
  onShow() { get('/api/live/rooms').then((rooms) => this.setData({ rooms })); },
  goRoom(e) { wx.navigateTo({ url: '/pages/liveroom/liveroom?id=' + e.currentTarget.dataset.id }); }
});
```

`pages/live/live.wxml`：直播间卡片（标题/封面占位）列表。

`pages/liveroom/liveroom.js`：

```javascript
const { get, post } = require('../../utils/request');

Page({
  data: { room: null, products: [], danmaku: [], input: '' },
  onLoad(options) {
    this.roomId = Number(options.id);
    this.load();
    this.timer = setInterval(() => this.loadDanmaku(), 3000);
  },
  onUnload() { clearInterval(this.timer); },
  load() { get('/api/live/rooms/' + this.roomId).then((d) => this.setData({ room: d.room, products: d.products })); },
  loadDanmaku() { get('/api/live/rooms/' + this.roomId + '/danmaku').then((danmaku) => this.setData({ danmaku })); },
  onInput(e) { this.setData({ input: e.detail.value }); },
  send() {
    const content = this.data.input.trim();
    if (!content) return;
    post('/api/live/rooms/' + this.roomId + '/danmaku', { content }).then(() => {
      this.setData({ input: '' });
      this.loadDanmaku();
    });
  },
  buy(e) {
    const p = this.data.products.find((x) => x.productId === e.currentTarget.dataset.id);
    wx.navigateTo({ url: '/pages/confirm/confirm?productId=' + p.productId + '&qty=1' });
  }
});
```

`pages/liveroom/liveroom.wxml`：`<video src="{{room.videoUrl}}">` 播放预录视频、弹幕滚动列表（nickname+content）、直播间商品轮播（名称+直播价+购买按钮）。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-live.js && node scripts/check.js`
预期：通过

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 直播列表与模拟直播间"
```

---

### 任务 8：RAG 客服聊天

**文件：**
- 修改：`miniprogram/pages/chat/chat.js/.wxml/.json`

- [ ] **步骤 1：冒烟脚本 `scripts/smoke-chat.js`**（校验 chat 含 send/onFeedback/onExample）

- [ ] **步骤 2：运行确认失败**

- [ ] **步骤 3：实现**

`pages/chat/chat.js`：

```javascript
const { get, post } = require('../../utils/request');

Page({
  data: { messages: [], input: '', conversationId: '', examples: ['柑橘怎么储存？', '泡菜发酵多久？', '助农补贴怎么申报？'] },
  onInput(e) { this.setData({ input: e.detail.value }); },
  onExample(e) { this.send(e.currentTarget.dataset.q); },
  send(question) {
    const q = question || this.data.input.trim();
    if (!q) return;
    const messages = this.data.messages.concat([{ role: 'user', content: q }]);
    this.setData({ messages, input: '' });
    post('/api/rag/chat', { question: q, conversationId: this.data.conversationId })
      .then((res) => {
        this.setData({
          conversationId: res.conversationId,
          messages: this.data.messages.concat([{
            role: 'assistant', content: res.answer,
            sources: res.sources || [], messageId: res.assistantMessageId, offline: res.offline
          }])
        });
      })
      .catch((err) => this.setData({ messages: this.data.messages.concat([{ role: 'assistant', content: '请求失败：' + err.message }]) }));
  },
  onFeedback(e) {
    const { id, fb } = e.currentTarget.dataset;
    post('/api/rag/messages/' + id + '/feedback', { feedback: fb }).catch(() => {});
    wx.showToast({ title: fb === 'UP' ? '已点赞' : '已点踩', icon: 'none' });
  }
});
```

`pages/chat/chat.wxml`：消息列表（用户右对齐/客服左对齐，来源用 `[来源:标题]` 显示并可展开原文）、示例问题标签、输入框 + 发送按钮、每条客服消息下"有用/没用"反馈。

- [ ] **步骤 4：运行验证**

运行：`cd miniprogram && node scripts/smoke-chat.js && node scripts/check.js`
预期：通过

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: RAG 客服聊天页"
```

---

### 任务 9：全量校验与演示脚本

**文件：**
- 修改：`miniprogram/scripts/check.js`（校验全部 13 个页面 .js/.wxml 存在且 .json 合法）
- 创建：`miniprogram/README.md`（运行说明）

- [ ] **步骤 1：编写最终校验脚本**

`scripts/check.js` 增强：遍历 `app.json.pages`，要求 `.js/.wxml/.json` 三件齐全且 `.json` 可解析；tabBar 页面已注册。

- [ ] **步骤 2：运行确认失败**

预期：FAIL（补齐缺失的 .json 或页面）

- [ ] **步骤 3：补齐并写运行说明**

`miniprogram/README.md`：

```markdown
# 眉山助农商城小程序

## 运行
1. 先启动后端（backend，端口 8080），确保 MySQL 已建库并导入种子数据。
2. 用微信开发者工具导入本目录（miniprogram/）。
3. 详情-本地设置勾选"不校验合法域名"，AppID 可使用测试号。
4. 登录：微信一键登录（未配置 AppID 时后端自动降级模拟登录）或手机号模拟登录（默认 13900000001，验证码任意）。

## 演示脚本
1. 登录进入首页
2. 首页/分类浏览商品
3. 直播列表进入直播间，发弹幕、点商品下单
4. 商品详情加购/购买 → 确认订单 → 模拟支付
5. 我的订单查看状态，模拟退款/确认收货
6. 商品详情"查看溯源"
7. 我的 → 在线客服 → 提问（带来源与反馈）
```

- [ ] **步骤 4：运行最终校验**

运行：`cd miniprogram && node scripts/check.js`
预期：CHECK_OK

- [ ] **步骤 5：Commit**

```bash
git add miniprogram
git commit -m "feat: 小程序全量校验与运行说明"
```

---

## 验收标准

1. `node scripts/check.js` 输出 CHECK_OK（13 个页面文件齐全、JSON 合法）。
2. 微信开发者工具导入后编译无报错，按 README 演示脚本 7 步可走通。
3. 后端未启动时请求层给出"网络异常"提示；token 失效自动回登录页。
