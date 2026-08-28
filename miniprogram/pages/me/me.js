const { logout } = require('../../utils/auth');

const ROLE = { CONSUMER: '消费者', SELLER: '商家', ADMIN: '管理员' };

Page({
  data: { user: {}, initial: '用', roleText: '' },
  onShow() {
    const user = wx.getStorageSync('user') || {};
    this.setData({
      user,
      initial: (user.nickname || '用').charAt(0),
      roleText: ROLE[user.role] || ''
    });
  },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }); },
  goAddress() { wx.navigateTo({ url: '/pages/address/address' }); },
  goChat() { wx.navigateTo({ url: '/pages/chat/chat' }); },
  onLogout() { logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});