const { baseUrl } = require('../config');

function request(path, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        satoken: wx.getStorageSync('satoken') || ''
      },
      success(res) {
        const body = res.data || {};
        if (body.code === 200) {
          resolve(body.data);
        } else if (body.code === 401) {
          wx.removeStorageSync('satoken');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(new Error(body.message || '未登录'));
        } else {
          reject(new Error(body.message || '请求失败'));
        }
      },
      fail(err) {
        reject(new Error('网络异常：' + (err.errMsg || '')));
      }
    });
  });
}

module.exports = {
  get: (p) => request(p),
  post: (p, d) => request(p, 'POST', d),
  put: (p, d) => request(p, 'PUT', d),
  del: (p) => request(p, 'DELETE')
};
