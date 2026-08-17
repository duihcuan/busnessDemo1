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
