const { logout } = require('../../utils/auth');

Page({
  data: { user: {} },
  onShow() { this.setData({ user: wx.getStorageSync('user') || {} }); },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }); },
  goAddress() { wx.navigateTo({ url: '/pages/address/address' }); },
  goChat() { wx.navigateTo({ url: '/pages/chat/chat' }); },
  onLogout() { logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});
