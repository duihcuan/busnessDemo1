const { get } = require('../../utils/request');

Page({
  data: { rooms: [] },
  onShow() { get('/api/live/rooms').then((rooms) => this.setData({ rooms })); },
  goRoom(e) { wx.navigateTo({ url: '/pages/liveroom/liveroom?id=' + e.currentTarget.dataset.id }); }
});
