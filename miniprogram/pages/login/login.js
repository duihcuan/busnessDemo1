const { wechatLogin, mockLogin } = require('../../utils/auth');

Page({
  data: { phone: '13900000001' },
  onPhone(e) { this.setData({ phone: e.detail.value }); },
  onWechatLogin() {
    wx.showLoading({ title: '登录中' });
    wechatLogin(this.data.phone)
      .then(() => { wx.hideLoading(); wx.switchTab({ url: '/pages/index/index' }); })
      .catch((err) => { wx.hideLoading(); wx.showToast({ title: err.message, icon: 'none' }); });
  },
  onMockLogin() {
    mockLogin(this.data.phone, '123456')
      .then(() => wx.switchTab({ url: '/pages/index/index' }))
      .catch((err) => wx.showToast({ title: err.message, icon: 'none' }));
  }
});
