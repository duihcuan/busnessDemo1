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
