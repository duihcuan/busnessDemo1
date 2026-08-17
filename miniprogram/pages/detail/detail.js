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
