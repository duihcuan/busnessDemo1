const { post } = require('./request');

function saveToken(res) {
  wx.setStorageSync('satoken', res.token);
  wx.setStorageSync('user', res);
}

function wechatLogin(phone) {
  return new Promise((resolve, reject) => {
    wx.login({
      success({ code }) {
        post('/api/auth/wechat-login', { code, phone })
          .then((res) => { saveToken(res); resolve(res); })
          .catch(reject);
      },
      fail: reject
    });
  });
}

function mockLogin(phone, code) {
  return post('/api/auth/mock-login', { phone, code })
    .then((res) => { saveToken(res); return res; });
}

function logout() {
  wx.removeStorageSync('satoken');
  wx.removeStorageSync('user');
}

module.exports = { wechatLogin, mockLogin, logout };
