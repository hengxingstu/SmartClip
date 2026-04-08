<template>
  <main class="app">
    <header class="topbar">
      <div>
        <h1>SmartClip</h1>
        <p>Local clipboard history for text you copy.</p>
      </div>
      <el-segmented v-model="activeTab" :options="tabs" />
    </header>

    <section v-if="activeTab === 'History'" class="panel">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="Search copied text" @keyup.enter="loadClips" />
        <el-button type="primary" @click="loadClips">Search</el-button>
        <el-button @click="loadClips">Refresh</el-button>
      </div>

      <el-table :data="clips" empty-text="No clips yet. Copy text and refresh.">
        <el-table-column prop="type" label="Type" width="180" />
        <el-table-column prop="previewText" label="Preview" min-width="320" show-overflow-tooltip />
        <el-table-column prop="copyCount" label="Count" width="100" />
        <el-table-column label="Last copied" width="190">
          <template #default="{ row }">{{ formatTime(row.lastCopiedAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row.id)">Details</el-button>
            <el-button size="small" @click="copy(row.id)">Copy</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="detailVisible" title="Clip details" width="70%">
        <template v-if="detail">
          <p class="meta">
            {{ detail.type }} | first {{ formatTime(detail.firstCopiedAt) }} | last
            {{ formatTime(detail.lastCopiedAt) }} | {{ detail.copyCount }} copies
          </p>
          <pre>{{ detail.content }}</pre>
        </template>
      </el-dialog>
    </section>

    <section v-else class="panel">
      <el-form v-if="settings" label-width="190px" class="settings-form">
        <el-form-item label="Listener">
          <el-switch v-model="settings.listenerEnabled" />
        </el-form-item>
        <el-form-item label="Poll interval ms">
          <el-input-number v-model="settings.pollIntervalMs" :min="300" />
        </el-form-item>
        <el-form-item label="Minimum text length">
          <el-input-number v-model="settings.minTextLength" :min="1" />
        </el-form-item>
        <el-form-item label="Ignore sensitive content">
          <el-switch v-model="settings.ignoreSensitiveEnabled" />
        </el-form-item>
        <el-button type="primary" @click="persistSettings">Save settings</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  type AppSettings,
  type ClipDetail,
  type ClipItem,
  copyClip,
  deleteClip,
  fetchClip,
  fetchClips,
  fetchSettings,
  saveSettings
} from './api'

const tabs = ['History', 'Settings']
const activeTab = ref('History')
const keyword = ref('')
const clips = ref<ClipItem[]>([])
const detail = ref<ClipDetail | null>(null)
const detailVisible = ref(false)
const settings = ref<AppSettings | null>(null)

async function loadClips() {
  const page = await fetchClips(keyword.value.trim())
  clips.value = page.items
}

async function openDetail(id: number) {
  detail.value = await fetchClip(id)
  detailVisible.value = true
}

async function copy(id: number) {
  await copyClip(id)
  ElMessage.success('Copied')
  await loadClips()
}

async function remove(id: number) {
  await deleteClip(id)
  ElMessage.success('Deleted')
  await loadClips()
}

async function loadSettings() {
  settings.value = await fetchSettings()
}

async function persistSettings() {
  if (!settings.value) return
  settings.value = await saveSettings(settings.value)
  ElMessage.success('Saved')
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

watch(activeTab, (tab) => {
  if (tab === 'Settings') {
    loadSettings().catch((error) => ElMessage.error(error.message))
  }
})

onMounted(() => {
  loadClips().catch((error) => ElMessage.error(error.message))
})
</script>
