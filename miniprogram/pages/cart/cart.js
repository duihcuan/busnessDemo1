const { get, put, del } = require('../../utils/request');

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
