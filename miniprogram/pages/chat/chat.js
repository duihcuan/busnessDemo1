const { post } = require('../../utils/request');

Page({
  data: { messages: [], input: '', conversationId: '', examples: ['柑橘怎么储存？', '泡菜发酵多久？', '助农补贴怎么申报？'] },
  onInput(e) { this.setData({ input: e.detail.value }); },
  onExample(e) { this.send(e.currentTarget.dataset.q); },
  onSend() { this.send(); },
  send(question) {
    const q = typeof question === 'string' && question.trim() ? question.trim() : this.data.input.trim();
    if (!q) return;
    const messages = this.data.messages.concat([{ role: 'user', content: q }]);
    this.setData({ messages, input: '' });
    post('/api/rag/chat', { question: q, conversationId: this.data.conversationId })
      .then((res) => {
        this.setData({
          conversationId: res.conversationId,
          messages: this.data.messages.concat([{
            role: 'assistant', content: res.answer || '抱歉，暂时没有获取到回答，请稍后再试。',
            sources: res.sources || [], messageId: res.assistantMessageId, offline: res.offline
          }])
        });
      })
      .catch((err) => this.setData({ messages: this.data.messages.concat([{ role: 'assistant', content: '请求失败：' + err.message }]) }));
  },
  onFeedback(e) {
    const { id, fb } = e.currentTarget.dataset;
    post('/api/rag/messages/' + id + '/feedback', { feedback: fb }).catch(() => {});
    wx.showToast({ title: fb === 'UP' ? '已点赞' : '已点踩', icon: 'none' });
  }
});
