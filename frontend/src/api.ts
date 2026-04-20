import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export type ClipListView = 'history' | 'favorites' | 'frequent' | 'ignored'

export interface TagItem {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}

export interface ClipItem {
  id: number
  type: string
  subType: string | null
  title: string
  previewText: string
  copyCount: number
  lastCopiedAt: string
  isFavorite: boolean
  isIgnored: boolean
  sensitivityLevel: string
  tags: TagItem[]
}

export interface ClipDetail extends ClipItem {
  content: string
  firstCopiedAt: string
  isIgnored: boolean
  createdAt: string
  updatedAt: string
}

export interface AppSettings {
  listenerEnabled: boolean
  pollIntervalMs: number
  minTextLength: number
  ignoreSensitiveEnabled: boolean
}

const client = axios.create({
  baseURL: '/api'
})

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  if (!response.data.success) {
    throw new Error(response.data.message || 'Request failed')
  }
  return response.data.data
}

export function fetchClips(keyword: string, type = '', view: ClipListView = 'history', tag = '') {
  return unwrap<PageResponse<ClipItem>>(client.get('/clips', {
    params: {
      keyword,
      type: type || undefined,
      view,
      tag: tag || undefined,
      page: 1,
      pageSize: 50
    }
  }))
}

export function fetchClip(id: number) {
  return unwrap<ClipDetail>(client.get(`/clips/${id}`))
}

export function copyClip(id: number) {
  return unwrap(client.post(`/clips/${id}/copy`))
}

export function deleteClip(id: number) {
  return unwrap(client.delete(`/clips/${id}`))
}

export function favoriteClip(id: number) {
  return unwrap(client.put(`/clips/${id}/favorite`))
}

export function unfavoriteClip(id: number) {
  return unwrap(client.delete(`/clips/${id}/favorite`))
}

export function restoreClip(id: number) {
  return unwrap(client.post(`/clips/${id}/restore`))
}

export function fetchSettings() {
  return unwrap<AppSettings>(client.get('/settings'))
}

export function saveSettings(settings: AppSettings) {
  return unwrap<AppSettings>(client.put('/settings', settings))
}

export function fetchTags(keyword = '') {
  return unwrap<PageResponse<TagItem>>(client.get('/tags', {
    params: {
      keyword: keyword || undefined,
      page: 1,
      pageSize: 20
    }
  }))
}

export function replaceClipTags(id: number, names: string[]) {
  return unwrap<TagItem[]>(client.put(`/clips/${id}/tags`, { names }))
}
