const { get, post, put, del } = require('../../utils/request');

Page({
  data: { list: [], form: {}, editing: false, showForm: false },
  onShow() { this.load(); },
  load() { get('/api/addresses').then((list) => this.setData({ list })); },
  openForm(e) {
    const item = e.currentTarget.dataset.item || {};
    this.setData({ showForm: true, editing: !!item.id, form: item });
  },
  onField(e) { this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value }); },
  onSave() {
    const f = this.data.form;
    const req = this.data.editing ? put('/api/addresses/' + f.id, f) : post('/api/addresses', f);
    req.then(() => { this.setData({ showForm: false }); this.load(); });
  },
  onSetDefault(e) {
    const item = this.data.list.find((x) => x.id === e.currentTarget.dataset.id);
    put('/api/addresses/' + item.id, { ...item, isDefault: 1 }).then(() => this.load());
  },
  onDelete(e) { del('/api/addresses/' + e.currentTarget.dataset.id).then(() => this.load()); },
  onCancelForm() { this.setData({ showForm: false }); }
});
