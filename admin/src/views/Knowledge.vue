<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="文档管理" name="docs">
      <el-button type="primary" @click="openDoc()">新增文档</el-button>
      <el-table :data="docs" style="margin-top:12px">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openDoc(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeDoc(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="docDialog" title="文档编辑" width="560px">
        <el-input v-model="docForm.title" placeholder="标题" style="margin-bottom:12px" />
        <el-input v-model="docForm.category" placeholder="分类（TECH/OPERATION/LIVE/POLICY/FAQ）" style="margin-bottom:12px" />
        <el-input v-model="docForm.source" placeholder="来源" style="margin-bottom:12px" />
        <el-input v-model="docForm.content" type="textarea" :rows="8" placeholder="正文（保存后自动切分入库）" />
        <template #footer><el-button type="primary" @click="saveDoc">保存</el-button></template>
      </el-dialog>
    </el-tab-pane>

    <el-tab-pane label="问答测试" name="test">
      <el-input v-model="question" placeholder="输入问题，如：柑橘怎么储存？" style="margin-bottom:12px" />
      <el-button type="primary" @click="testChat">提问</el-button>
      <el-card v-if="answer" style="margin-top:12px">
        <div>{{ answer.answer }}</div>
        <div v-if="answer.offline" class="tag">离线回答</div>
        <div v-for="s in answer.sources" :key="s.docId">[来源:{{ s.title }}]</div>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="用户反馈" name="feedback">
      <el-table :data="messages">
        <el-table-column prop="role" label="角色" width="90" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="feedback" label="反馈" width="90" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '../api/request';

const tab = ref('docs');
const docs = ref([]);
const docDialog = ref(false);
const docForm = ref({});
const question = ref('');
const answer = ref(null);
const messages = ref([]);

async function loadDocs() {
  const page = await request.get('/rag/documents?page=1&size=100');
  docs.value = page.records;
}
function openDoc(row) { docForm.value = row ? { ...row } : {}; docDialog.value = true; }
async function saveDoc() {
  if (docForm.value.id) await request.put('/rag/documents/' + docForm.value.id, docForm.value);
  else await request.post('/rag/documents', docForm.value);
  docDialog.value = false;
  loadDocs();
}
async function removeDoc(row) { await request.delete('/rag/documents/' + row.id); loadDocs(); }
async function testChat() {
  answer.value = await request.post('/rag/chat', { question: question.value });
}
async function loadMessages() {
  const page = await request.get('/rag/messages?page=1&size=100');
  messages.value = page.records;
}
onMounted(() => {
  loadDocs();
  loadMessages();
});
</script>
