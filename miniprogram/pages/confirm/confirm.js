const { get, post } = require('../../utils/request');

Page({
  data: { productId: 0, qty: 1, liveProductId: 0, product: null, cartItems: [], address: null, addresses: [] },
  onLoad(options) {
    if (options.productId) {
      this.setData({ productId: Number(options.productId), qty: Number(options.qty || 1), liveProductId: Number(options.liveProductId || 0) });
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
      ? [{ productId: this.data.productId, quantity: this.data.qty, liveProductId: this.data.liveProductId || undefined }]
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
