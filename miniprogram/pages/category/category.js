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
