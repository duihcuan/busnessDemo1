App({
  onLaunch() {
    if (!wx.getStorageSync('satoken')) {
      wx.reLaunch({ url: '/pages/login/login' });
    }
  }
});
