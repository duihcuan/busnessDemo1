const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

const appJson = readJson(path.join(root, 'app.json'));
const errors = [];
for (const page of appJson.pages) {
  for (const ext of ['.js', '.wxml', '.json']) {
    const file = path.join(root, page + ext);
    if (!fs.existsSync(file)) {
      errors.push('缺少页面文件: ' + page + ext);
    } else if (ext === '.json') {
      try { readJson(file); } catch (e) { errors.push('JSON 非法: ' + page + ext); }
    }
  }
}
for (const item of appJson.tabBar.list) {
  if (!appJson.pages.includes(item.pagePath)) errors.push('tabBar 页面未注册: ' + item.pagePath);
}
if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log('CHECK_OK: 所有页面文件齐全，JSON 合法');
