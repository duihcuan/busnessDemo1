Page({
  data: { categories: [], products: [] },
  onShow() {
    const { get } = require('../../utils/request');
    get('/api/categories').then((categories) => this.setData({ categories }));
    get('/api/products?page=1&size=6').then((page) => this.setData({ products: page.records }));
  },
  goCategory() { wx.switchTab({ url: '/pages/category/category' }); },
  goLive() { wx.navigateTo({ url: '/pages/live/live' }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
