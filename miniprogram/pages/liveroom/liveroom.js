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
