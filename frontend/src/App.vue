<template>
  <main class="app-shell">
    <section v-if="!isSettingsView" class="history-card">
      <div class="history-hero">
        <div class="hero-overlay">
          <div class="hero-top">
            <div class="brand-block">
              <h1>SmartClip</h1>
              <p>Keep the text you copy close at hand.</p>
            </div>
            <div class="hero-actions">
              <button class="icon-button" type="button" aria-label="Settings" @click="openSettings">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 8.8A3.2 3.2 0 1 1 8.8 12 3.2 3.2 0 0 1 12 8.8Z" />
                  <path
                    d="M19.4 13.1a7.6 7.6 0 0 0 .05-2.18l1.73-1.35-1.68-2.9-2.16.61a7.88 7.88 0 0 0-1.88-1.1L15.14 4h-3.28l-.36 2.19a7.88 7.88 0 0 0-1.88 1.1l-2.16-.61-1.68 2.9 1.73 1.35a7.6 7.6 0 0 0 .05 2.18l-1.73 1.35 1.68 2.9 2.16-.61a7.88 7.88 0 0 0 1.88 1.1l.36 2.19h3.28l.36-2.19a7.88 7.88 0 0 0 1.88-1.1l2.16.61 1.68-2.9-1.73-1.35Z"
                  />
                </svg>
              </button>
            </div>
          </div>

          <div class="hero-content">
            <div class="hero-metric">
              <span class="metric-value">{{ totalClips }}</span>
              <span class="metric-unit">clips</span>
            </div>
            <p class="hero-status">{{ summaryTitle }}</p>
            <p class="hero-meta">{{ summaryMeta }}</p>
          </div>
        </div>
      </div>

      <div class="history-sheet">
        <div class="view-switcher">
          <button
            v-for="item in listViewOptions"
            :key="item.value"
            type="button"
            class="view-chip"
            :class="{ active: activeView === item.value }"
            @click="selectView(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="sheet-toolbar">
          <el-input
            v-model="keyword"
            clearable
            placeholder="Search copied text"
            @keyup.enter="loadClips"
          />
          <el-select
            v-model="selectedType"
            clearable
            placeholder="All types"
            @change="loadClips"
          >
            <el-option
              v-for="item in clipTypeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <el-select
            v-model="selectedTag"
            filterable
            clearable
            remote
            reserve-keyword
            placeholder="Filter by tag"
            :remote-method="searchTagOptions"
            :loading="tagOptionsLoading"
            @visible-change="handleTagFilterVisible"
            @change="loadClips"
          >
            <el-option
              v-for="tag in tagOptions"
              :key="tag.id"
              :label="tag.name"
              :value="tag.name"
            />
          </el-select>
          <el-button @click="loadClips">Refresh</el-button>
        </div>

        <div v-if="selectedTag" class="active-filter-row">
          <span class="active-filter-label">Tag filter</span>
          <button class="active-tag-chip" type="button" @click="clearTagFilter">
            {{ selectedTag }}
            <span aria-hidden="true">×</span>
          </button>
        </div>

        <div v-if="clips.length" class="clip-list">
          <article v-for="clip in clips" :key="clip.id" class="clip-item">
            <div class="clip-main">
              <div class="clip-type-row">
                <div class="clip-type">{{ clip.type }}</div>
                <span v-if="clip.subType" class="clip-subtype">{{ formatSubtype(clip.subType) }}</span>
                <span v-if="clip.isFavorite" class="clip-badge">Favorite</span>
              </div>
              <h3 class="clip-title">{{ clip.title || clip.previewText }}</h3>
              <p v-if="shouldShowPreview(clip)" class="clip-preview">{{ clip.previewText }}</p>
              <div v-if="clip.tags?.length" class="clip-tag-row">
                <button
                  v-for="tag in clip.tags"
                  :key="tag.id"
                  type="button"
                  class="clip-tag"
                  @click="applyTagFilter(tag.name)"
                >
                  {{ tag.name }}
                </button>
              </div>
              <div class="clip-meta-row">
                <span>{{ clip.copyCount }} copies</span>
                <span>{{ formatTime(clip.lastCopiedAt) }}</span>
              </div>
            </div>
            <div class="clip-actions">
              <el-button size="small" @click="openDetail(clip.id)">Details</el-button>
              <template v-if="activeView !== 'ignored'">
                <el-button size="small" @click="copy(clip.id)">Copy</el-button>
                <el-button size="small" @click="toggleFavorite(clip)">
                  {{ clip.isFavorite ? 'Unfavorite' : 'Favorite' }}
                </el-button>
                <el-button size="small" type="danger" @click="ignoreClip(clip.id)">Ignore</el-button>
              </template>
              <el-button v-else size="small" type="primary" @click="restoreIgnored(clip.id)">
                Restore
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <p>{{ emptyTitle }}</p>
          <span>{{ emptyDescription }}</span>
        </div>
      </div>
    </section>

    <section v-else class="settings-panel">
      <div class="settings-topbar">
        <div>
          <h2>Settings</h2>
          <p>Control the local clipboard listener and save rules.</p>
        </div>
        <el-button @click="backToListView">Back</el-button>
      </div>

      <el-form v-if="settings" label-width="190px" class="settings-form">
        <el-form-item label="Listener">
          <div class="setting-field">
            <el-switch v-model="settings.listenerEnabled" />
            <p>Turn clipboard polling on or off without restarting the app.</p>
          </div>
        </el-form-item>
        <el-form-item label="Poll interval ms">
          <div class="setting-field">
            <el-input-number v-model="settings.pollIntervalMs" :min="300" />
            <p>Lower values react faster but poll the clipboard more often.</p>
          </div>
        </el-form-item>
        <el-form-item label="Minimum text length">
          <div class="setting-field">
            <el-input-number v-model="settings.minTextLength" :min="1" />
            <p>Ignore very short copied text that is usually accidental or low value.</p>
          </div>
        </el-form-item>
        <el-form-item label="Ignore sensitive content">
          <div class="setting-field">
            <el-switch v-model="settings.ignoreSensitiveEnabled" />
            <p>Skip content that looks like passwords, tokens, keys, or secrets.</p>
          </div>
        </el-form-item>

        <div class="settings-actions">
          <el-button type="primary" @click="persistSettings">Save settings</el-button>
          <el-button @click="resetSettings">Restore defaults</el-button>
        </div>
      </el-form>
    </section>

    <el-dialog v-model="detailVisible" title="Clip details" width="min(760px, 92vw)" class="detail-dialog">
      <template v-if="detail">
        <p class="detail-meta">
          {{ detail.type }}<template v-if="detail.subType"> / {{ formatSubtype(detail.subType) }}</template>
          | first {{ formatTime(detail.firstCopiedAt) }} | last
          {{ formatTime(detail.lastCopiedAt) }} | {{ detail.copyCount }} copies
        </p>

        <section class="detail-section">
          <div class="detail-section-header">
            <div>
              <h3>Tags</h3>
              <p>Edit tags for this clip or add a new one directly from here.</p>
            </div>
            <el-button
              type="primary"
              :loading="tagSaving"
              @click="saveDetailTags"
            >
              Save tags
            </el-button>
          </div>

          <el-select
            v-model="detailTagNames"
            class="detail-tag-editor"
            multiple
            filterable
            remote
            allow-create
            default-first-option
            reserve-keyword
            placeholder="Type a tag and press Enter"
            :remote-method="searchTagOptions"
            :loading="tagOptionsLoading"
            @visible-change="handleDetailTagVisible"
          >
            <el-option
              v-for="tag in tagOptions"
              :key="tag.id"
              :label="tag.name"
              :value="tag.name"
            />
          </el-select>

          <div v-if="detail.tags?.length" class="detail-tag-list">
            <button
              v-for="tag in detail.tags"
              :key="tag.id"
              type="button"
              class="detail-tag-chip"
              @click="applyTagFilterFromDetail(tag.name)"
            >
              {{ tag.name }}
            </button>
          </div>
        </section>

        <pre>{{ detail.content }}</pre>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  type AppSettings,
  type ClipDetail,
  type ClipItem,
  type ClipListView,
  type TagItem,
  favoriteClip,
  copyClip,
  deleteClip,
  fetchClip,
  fetchClips,
  fetchSettings,
  fetchTags,
  replaceClipTags,
  restoreClip,
  saveSettings,
  unfavoriteClip
} from './api'

type AppView = ClipListView | 'settings'

const defaultSettings: AppSettings = {
  listenerEnabled: true,
  pollIntervalMs: 1000,
  minTextLength: 3,
  ignoreSensitiveEnabled: false
}

const listViewOptions: Array<{ label: string; value: ClipListView }> = [
  { label: 'History', value: 'history' },
  { label: 'Favorites', value: 'favorites' },
  { label: 'Frequent', value: 'frequent' },
  { label: 'Ignored', value: 'ignored' }
]

const clipTypeOptions = ['URL', 'JSON', 'SQL', 'COMMAND', 'JAVA_EXCEPTION_LOG', 'FILE_PATH', 'CODE', 'TEXT']

const activeView = ref<AppView>('history')
const lastListView = ref<ClipListView>('history')
const keyword = ref('')
const selectedType = ref('')
const selectedTag = ref('')
const clips = ref<ClipItem[]>([])
const totalClips = ref(0)
const detail = ref<ClipDetail | null>(null)
const detailVisible = ref(false)
const detailTagNames = ref<string[]>([])
const settings = ref<AppSettings | null>(null)
const tagOptions = ref<TagItem[]>([])
const tagOptionsLoading = ref(false)
const tagSaving = ref(false)

const isSettingsView = computed(() => activeView.value === 'settings')

const currentListView = computed<ClipListView>(() =>
  activeView.value === 'settings' ? lastListView.value : activeView.value
)

const currentViewLabel = computed(() => {
  const active = listViewOptions.find((item) => item.value === currentListView.value)
  return active?.label ?? 'History'
})

const summaryTitle = computed(() => {
  if (!clips.value.length) {
    return currentViewLabel.value
  }
  if (currentListView.value === 'frequent') {
    return `Top ${clips.value[0].type}`
  }
  return clips.value[0].type
})

const summaryMeta = computed(() => {
  if (!clips.value.length) {
    return 'Your copied text will appear here after the next refresh.'
  }
  const filterMeta = selectedTag.value ? ` | tag ${selectedTag.value}` : ''
  if (currentListView.value === 'frequent') {
    return `${clips.value[0].copyCount} copies | latest ${formatTime(clips.value[0].lastCopiedAt)}${filterMeta}`
  }
  return `Latest saved at ${formatTime(clips.value[0].lastCopiedAt)}${filterMeta}`
})

const emptyTitle = computed(() => {
  if (selectedTag.value) {
    return 'No clips match this tag.'
  }
  switch (currentListView.value) {
    case 'favorites':
      return 'No favorites yet.'
    case 'frequent':
      return 'No frequent clips yet.'
    case 'ignored':
      return 'No ignored clips.'
    default:
      return 'No clips yet.'
  }
})

const emptyDescription = computed(() => {
  if (selectedTag.value) {
    return 'Try another tag or clear the current filter.'
  }
  switch (currentListView.value) {
    case 'favorites':
      return 'Mark important clips as favorites to keep them close.'
    case 'frequent':
      return 'Frequently copied clips will rise here once you use them more.'
    case 'ignored':
      return 'Ignored clips can be restored back into your normal history.'
    default:
      return 'Copy some text and refresh this view.'
  }
})

async function loadClips() {
  const page = await fetchClips(keyword.value.trim(), selectedType.value, currentListView.value, selectedTag.value.trim())
  clips.value = page.items
  totalClips.value = page.total
}

function selectView(view: ClipListView) {
  activeView.value = view
}

function openSettings() {
  activeView.value = 'settings'
}

function backToListView() {
  activeView.value = lastListView.value
}

async function openDetail(id: number) {
  detail.value = await fetchClip(id)
  detailTagNames.value = detail.value.tags.map((tag) => tag.name)
  detailVisible.value = true
  await ensureTagOptions()
}

async function copy(id: number) {
  await copyClip(id)
  ElMessage.success('Copied')
  await loadClips()
}

async function ignoreClip(id: number) {
  await deleteClip(id)
  ElMessage.success('Ignored')
  await loadClips()
}

async function toggleFavorite(clip: ClipItem) {
  if (clip.isFavorite) {
    await unfavoriteClip(clip.id)
    ElMessage.success('Removed from favorites')
  } else {
    await favoriteClip(clip.id)
    ElMessage.success('Added to favorites')
  }
  await loadClips()
}

async function restoreIgnored(id: number) {
  await restoreClip(id)
  ElMessage.success('Restored')
  await loadClips()
}

async function loadSettings() {
  settings.value = await fetchSettings()
}

async function persistSettings() {
  if (!settings.value) return
  settings.value = await saveSettings(settings.value)
  ElMessage.success('Settings saved')
}

function resetSettings() {
  settings.value = { ...defaultSettings }
  ElMessage.info('Defaults restored. Save to apply them.')
}

async function searchTagOptions(keywordValue: string) {
  tagOptionsLoading.value = true
  try {
    const page = await fetchTags(keywordValue.trim())
    tagOptions.value = page.items
  } finally {
    tagOptionsLoading.value = false
  }
}

async function ensureTagOptions() {
  if (!tagOptions.value.length) {
    await searchTagOptions('')
  }
}

function handleTagFilterVisible(visible: boolean) {
  if (visible) {
    ensureTagOptions().catch((error: Error) => ElMessage.error(error.message))
  }
}

function handleDetailTagVisible(visible: boolean) {
  if (visible) {
    ensureTagOptions().catch((error: Error) => ElMessage.error(error.message))
  }
}

function normalizeTagNames(names: string[]) {
  return Array.from(
    new Map(
      names
        .map((name) => name.trim())
        .filter(Boolean)
        .map((name) => [name.toLowerCase(), name] as const)
    ).values()
  )
}

async function saveDetailTags() {
  if (!detail.value) return

  tagSaving.value = true
  try {
    const names = normalizeTagNames(detailTagNames.value)
    const tags = await replaceClipTags(detail.value.id, names)
    detailTagNames.value = tags.map((tag) => tag.name)
    detail.value = { ...detail.value, tags }
    ElMessage.success('Tags updated')
    await ensureTagOptions()
    await loadClips()
    if (detail.value) {
      detail.value = await fetchClip(detail.value.id)
      detailTagNames.value = detail.value.tags.map((tag) => tag.name)
    }
  } finally {
    tagSaving.value = false
  }
}

function applyTagFilter(name: string) {
  selectedTag.value = name
  loadClips().catch((error: Error) => ElMessage.error(error.message))
}

function applyTagFilterFromDetail(name: string) {
  selectedTag.value = name
  detailVisible.value = false
  loadClips().catch((error: Error) => ElMessage.error(error.message))
}

function clearTagFilter() {
  selectedTag.value = ''
  loadClips().catch((error: Error) => ElMessage.error(error.message))
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

function formatSubtype(value: string) {
  return value
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char: string) => char.toUpperCase())
}

function shouldShowPreview(clip: ClipItem) {
  const title = (clip.title || '').trim()
  const preview = (clip.previewText || '').trim()
  return Boolean(preview) && title !== preview
}

watch(activeView, (view) => {
  if (view === 'settings') {
    loadSettings().catch((error: Error) => ElMessage.error(error.message))
    return
  }
  lastListView.value = view
  loadClips().catch((error: Error) => ElMessage.error(error.message))
})

watch(detailVisible, (visible) => {
  if (!visible) {
    detailTagNames.value = []
  }
})

onMounted(() => {
  loadClips().catch((error: Error) => ElMessage.error(error.message))
})
</script>
