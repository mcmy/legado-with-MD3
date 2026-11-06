<template>
  <div
    class="chapter-wrapper"
    :style="bodyTheme"
    :class="{ night: isNight, day: !isNight }"
    @click="showToolBar = !showToolBar"
  >
    <div class="tool-bar" :style="leftBarTheme">
      <div class="tools">
        <el-popover
          placement="right"
          :width="popupWidth"
          trigger="click"
          :show-arrow="false"
          v-model:visible="popCataVisible"
          popper-class="pop-cata"
        >
          <PopCatalog @getContent="getContent" class="popup" />
          <template #reference>
            <div class="tool-icon" :class="{ 'no-point': false }">
              <div class="iconfont">&#58905;</div>
              <div class="icon-text">目录</div>
            </div>
          </template>
        </el-popover>
        <el-popover
          placement="right"
          :width="popupWidth"
          trigger="click"
          :show-arrow="false"
          v-model:visible="readSettingsVisible"
          popper-class="pop-setting"
        >
          <read-settings class="popup" />
          <template #reference>
            <div class="tool-icon" :class="{ 'no-point': noPoint }">
              <div class="iconfont">&#58971;</div>
              <div class="icon-text">设置</div>
            </div>
          </template>
        </el-popover>
        <div class="tool-icon" @click="toShelf">
          <div class="iconfont">&#58892;</div>
          <div class="icon-text">书架</div>
        </div>
        <div class="tool-icon" :class="{ 'no-point': noPoint }" @click="toTop">
          <div class="iconfont">&#58914;</div>
          <div class="icon-text">顶部</div>
        </div>
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toBottom"
        >
          <div class="iconfont">&#58915;</div>
          <div class="icon-text">底部</div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="rightBarTheme">
      <div class="tools">
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toPreChapter"
        >
          <div class="iconfont">&#58920;</div>
          <span v-if="miniInterface">上一章</span>
        </div>
        <div
          class="tool-icon"
          :class="{ 'no-point': noPoint }"
          @click="toNextChapter"
        >
          <span v-if="miniInterface">下一章</span>
          <div class="iconfont">&#58913;</div>
        </div>
      </div>
    </div>
    <div class="chapter-bar"></div>
    <div class="chapter" ref="content" :style="chapterTheme">
      <div class="content">
        <div class="top-bar" ref="top"></div>
        <div ref="leadingSpacer" :style="{ height: `${spacerHeights.before}px` }" aria-hidden="true"></div>
        <div
          v-for="data in chapterData"
          :key="data.index"
          :data-reader-chapter="data.index"
          ref="chapter"
        >
          <chapter-content
            ref="chapterRef"
            :chapterIndex="data.index"
            :contents="data.content"
            :title="data.title"
            :spacing="store.config.spacing"
            :fontSize="fontSize"
            :fontFamily="fontFamily"
            @readedLengthChange="onReadedLengthChange"
            v-if="showContent"
          />
        </div>
        <div ref="trailingSpacer" :style="{ height: `${spacerHeights.after}px` }" aria-hidden="true"></div>
        <div class="loading" ref="loading"></div>
        <div class="bottom-bar" ref="bottom"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import jump from '@/plugins/jump'
import settings from '@/config/themeConfig'
import API, { type ReaderWebParagraph } from '@api'
import { useLoading } from '@/hooks/loading'
import { useThrottleFn } from '@vueuse/shared'
import { isNullOrBlank } from '@/utils/utils'
import { initXboxGamepad } from '@/utils/xboxGamepad'
import { createChapterPrefetcher } from '@/utils/chapterPrefetch'
import { normalizeSpaceScrollDuration, spaceScrollDistance } from '@/utils/spaceScrollDuration'
import {
  chapterWindowIndices, chapterSpacerHeights, captureChapterAnchor,
  restoreChapterAnchor, scrollChapterBy, type ScrollAnchor,
} from '@/utils/chapterWindow'

const content = ref<HTMLElement>()
// loading spinner
const { isLoading, loadingWrapper } = useLoading(content, '正在获取信息')
const store = useBookStore()

const {
  catalog,
  popCataVisible,
  readSettingsVisible,
  miniInterface,
  showContent,
  bookProgress,
  theme,
  isNight,
} = storeToRefs(store)

const chapterPos = computed({
  get: () => store.readingBook.chapterPos,
  set: value => (store.readingBook.chapterPos = value),
})
const chapterIndex = computed({
  get: () => store.readingBook.chapterIndex,
  set: value => (store.readingBook.chapterIndex = value),
})
const isSeachBook = computed({
  get: () => store.readingBook.isSeachBook,
  set: value => (store.readingBook.isSeachBook = value),
})

// 当前阅读书籍readingBook持久化
watch(
  () => store.readingBook,
  book => {
    // 保存localStorage
    // localStorage.setItem(book.bookUrl, JSON.stringify(book));
    // 最近阅读
    localStorage.setItem('readingRecent', JSON.stringify(book))
    //保存 sessionStorage
    sessionStorage.setItem('chapterIndex', book.chapterIndex.toString())
    sessionStorage.setItem('chapterPos', book.chapterPos.toString())
  },
  { deep: 1 },
)

// 无限滚动
const infiniteLoading = computed(() => store.config.infiniteLoading)
let scrollObserver: IntersectionObserver | null
const loading = ref()
watchEffect(() => {
  if (!infiniteLoading.value) {
    scrollObserver?.disconnect()
  } else {
    scrollObserver?.observe(loading.value)
  }
})
const loadMore = () => {
  if (windowReady) refreshChapterWindow(chapterIndex.value)
}
// IntersectionObserver回调 底部加载
const onReachBottom = (entries: IntersectionObserverEntry[]) => {
  if (isLoading.value) return
  for (const { isIntersecting } of entries) {
    if (!isIntersecting) return
    loadMore()
  }
}

// 自动翻页（滚动模式，行为对齐App端）
const autoPagePaused = ref(false)
let autoPageRaf = 0
let autoPageLastFrame = 0
let autoPageResumeTimer: number | undefined

const READ_SESSION_FLUSH_INTERVAL_MS = 60_000
const MIN_READ_SESSION_MS = 10_000
const MAX_READ_SESSION_MS = 90_000
let readSessionStartTime = 0
let readSessionFlushTimer: number | undefined

const buildReadSession = (startTime: number, endTime: number) => ({
  bookName: store.readingBook.name,
  bookAuthor: store.readingBook.author,
  bookUrl: store.readingBook.bookUrl,
  startTime,
  endTime,
  chapterIndex: chapterIndex.value,
})

const beginReadSession = () => {
  if (
    readSessionStartTime === 0 &&
    document.visibilityState === 'visible' &&
    store.showContent &&
    store.readingBook.name
  ) {
    readSessionStartTime = Date.now()
  }
}

/**
 * 将当前可见阅读片段保存为一个不可重叠的 session。
 * 单段最多记 90 秒，避免设备休眠后定时器恢复时把休眠时间算作阅读。
 */
const flushReadSession = () => {
  const startTime = readSessionStartTime
  if (startTime === 0) return
  const now = Date.now()
  const endTime = Math.min(now, startTime + MAX_READ_SESSION_MS)
  if (endTime - startTime < MIN_READ_SESSION_MS) return
  const session = buildReadSession(startTime, endTime)
  if (store.saveReadSessionWithBeacon(session)) {
    readSessionStartTime = now
  }
}

const stopReadSession = () => {
  if (readSessionFlushTimer !== undefined) {
    window.clearInterval(readSessionFlushTimer)
    readSessionFlushTimer = undefined
  }
  flushReadSession()
  readSessionStartTime = 0
}

/**
 * 自动翻页速度（毫秒/页）。防御历史配置或手改 JSON：非法值回落到默认 10 秒/页，
 * 避免 0 造成 `innerHeight / 0` 一次性滚到章末。
 */
const autoPageDurationMs = () => {
  const seconds = Number(store.config.autoPageSpeed)
  return (
    Math.min(120, Math.max(1, Number.isFinite(seconds) ? seconds : 10)) * 1000
  )
}

/**
 * 章末判定。要求内容超过一屏，避免首屏尚未加载时把空页面误判为已到章末。
 */
const atChapterEnd = () =>
  document.documentElement.scrollHeight > window.innerHeight + 1 &&
  window.innerHeight + window.scrollY >=
    document.documentElement.scrollHeight - 1

const startAutoPage = () => {
  if (autoPageRaf || !store.config.autoPage) return
  // 弹窗打开期间不启动滚动（细节5：目录/设置弹窗打开时暂停）
  if (popCataVisible.value || readSettingsVisible.value) {
    autoPagePaused.value = true
    return
  }
  autoPageLastFrame = 0
  const tick = (now: number) => {
    if (!store.config.autoPage || autoPagePaused.value) {
      autoPageRaf = 0
      return
    }
    if (autoPageLastFrame === 0) autoPageLastFrame = now
    const elapsed = now - autoPageLastFrame
    autoPageLastFrame = now
    // 与App端一致：每帧滚动 视口高度 / (速度秒数 × 1000ms) × 本帧毫秒数
    window.scrollBy(0, (window.innerHeight / autoPageDurationMs()) * elapsed)
    // 未开启无限加载时章末不会再有新内容：停在章末，不继续空转
    if (!infiniteLoading.value && atChapterEnd()) {
      pauseAutoPage(0)
      return
    }
    autoPageRaf = requestAnimationFrame(tick)
  }
  autoPageRaf = requestAnimationFrame(tick)
}
const stopAutoPage = () => {
  if (autoPageRaf) cancelAnimationFrame(autoPageRaf)
  autoPageRaf = 0
}
const resumeAutoPage = () => {
  if (autoPageResumeTimer) {
    window.clearTimeout(autoPageResumeTimer)
    autoPageResumeTimer = undefined
  }
  if (!store.config.autoPage) return
  autoPagePaused.value = false
  startAutoPage()
}
/**
 * 暂停自动翻页
 * @param idleResumeMs 大于0时在用户停止交互一段时间后自动恢复；0表示保持暂停直到手动恢复
 */
const pauseAutoPage = (idleResumeMs = 3000) => {
  if (autoPageResumeTimer) {
    window.clearTimeout(autoPageResumeTimer)
    autoPageResumeTimer = undefined
  }
  if (!store.config.autoPage) return
  autoPagePaused.value = true
  stopAutoPage()
  if (idleResumeMs > 0) {
    autoPageResumeTimer = window.setTimeout(resumeAutoPage, idleResumeMs)
  }
}
// 开关切换
watch(
  () => store.config.autoPage,
  on => (on ? startAutoPage() : stopAutoPage()),
)
// 打开目录/设置弹窗时暂停，关闭后恢复
watch([popCataVisible, readSettingsVisible], ([cata, settings]) => {
  if (cata || settings) {
    pauseAutoPage(0)
  } else {
    resumeAutoPage()
  }
})
// 用户滚轮/触摸交互时暂停，停止操作一段时间后自动恢复（touchmove 保证长按拖动期间持续暂停）
const onUserWheel = () => {
  pauseAutoPage()
  onReaderScroll()
}
const onUserTouch = () => pauseAutoPage()
const onUserTouchMove = () => {
  pauseAutoPage()
  onReaderScroll()
}

// 字体
const fontFamily = computed(() => {
  if (store.config.font >= 0) {
    return settings.fonts[store.config.font]
  }
  return store.config.customFontName
})
const fontSize = computed(() => {
  return store.config.fontSize + 'px'
})

// 主题部分
const bodyColor = computed(() => settings.themes[theme.value].body)
const chapterColor = computed(() => settings.themes[theme.value].content)
const popupColor = computed(() => settings.themes[theme.value].popup)

const readWidth = computed(() => {
  if (!miniInterface.value) {
    return store.config.readWidth - 130 + 'px'
  } else {
    return window.innerWidth + 'px'
  }
})
const popupWidth = computed(() => {
  if (!miniInterface.value) {
    return store.config.readWidth - 33
  } else {
    return window.innerWidth - 33
  }
})
const bodyTheme = computed(() => {
  return {
    background: bodyColor.value,
  }
})
const chapterTheme = computed(() => {
  return {
    background: chapterColor.value,
    width: readWidth.value,
  }
})
const showToolBar = ref(false)
const leftBarTheme = computed(() => {
  return {
    background: popupColor.value,
    marginLeft: miniInterface.value
      ? 0
      : -(store.config.readWidth / 2 + 68) + 'px',
    display: miniInterface.value && !showToolBar.value ? 'none' : 'block',
  }
})
const rightBarTheme = computed(() => {
  return {
    background: popupColor.value,
    marginRight: miniInterface.value
      ? 0
      : -(store.config.readWidth / 2 + 52) + 'px',
    display: miniInterface.value && !showToolBar.value ? 'none' : 'block',
  }
})

/**
 * pc移动端判断 最大阅读宽度修正
 * 阅读宽度最小为640px 加上工具栏 68px 52px 取较大值 为 776px
 */
const onResize = () => {
  store.setMiniInterface(window.innerWidth < 776)
  const width = store.config.readWidth /**包含padding */
  checkPageWidth(width)
}
/** 判断阅读宽度是否超出页面或者低于默认值640 */
const checkPageWidth = (readWidth: number) => {
  if (store.miniInterface) return
  if (readWidth < 640) store.config.readWidth = 640
  if (readWidth + 2 * 68 > window.innerWidth) store.config.readWidth -= 160
}
watch(
  () => store.config.readWidth,
  width => checkPageWidth(width),
)
// 顶部底部跳转
const top = ref()
const bottom = ref()
const toTop = () => {
  pauseAutoPage()
  jump(top.value)
}
const toBottom = () => {
  pauseAutoPage()
  jump(bottom.value)
}

// 书架路由切换
const router = useRouter()
const toShelf = () => {
  router.push('/')
}

// 获取章节内容
type DisplayChapter = { index: number; content: ReaderWebParagraph[]; title: string }
const chapterData = ref<DisplayChapter[]>([])
const spacerHeights = ref({ before: 0, after: 0 })
const leadingSpacer = ref<HTMLElement>()
const trailingSpacer = ref<HTMLElement>()
const chapterHeights = new Map<number, number>()
let windowCenter = -1
let windowReady = false
let lastAnchor: ScrollAnchor | undefined
let chapterResizeObserver: ResizeObserver | undefined
let scrollFrame = 0
let layoutQueue = Promise.resolve()
const pendingWindowChapters = new Map<number, Promise<unknown>>()

const measureChapters = () => {
  for (const element of content.value?.querySelectorAll<HTMLElement>('[data-reader-chapter]') ?? []) {
    chapterHeights.set(Number(element.dataset.readerChapter), element.getBoundingClientRect().height)
  }
}

// Vue's DOM patch and the offset correction complete in the same frame, before paint.
const updateChapterLayout = (transform: (items: DisplayChapter[]) => DisplayChapter[]) => {
  const generation = contentGeneration
  const update = layoutQueue.then(async () => {
    if (readerDisposed || generation !== contentGeneration) return
    const anchor = captureChapterAnchor(content.value)
    measureChapters()
    const allowed = infiniteLoading.value
      ? chapterWindowIndices(windowCenter, catalog.value.length) : [windowCenter]
    chapterData.value = transform(chapterData.value)
      .filter(item => allowed.includes(item.index)).sort((a, b) => a.index - b.index)
    const first = chapterData.value[0]?.index ?? windowCenter
    const last = chapterData.value.slice(-1)[0]?.index ?? windowCenter
    spacerHeights.value = infiniteLoading.value
      ? chapterSpacerHeights(chapterHeights, first, last) : { before: 0, after: 0 }
    await nextTick()
    if (readerDisposed || generation !== contentGeneration) return
    restoreChapterAnchor(anchor)
    measureChapters()
    chapterResizeObserver?.disconnect()
    for (const element of content.value?.querySelectorAll<HTMLElement>('[data-reader-chapter]') ?? []) {
      chapterResizeObserver?.observe(element)
    }
    lastAnchor = captureChapterAnchor(content.value)
  })
  layoutQueue = update.catch(() => {})
  return update
}

const displayChapter = (index: number, data: { title: string; paragraphs: ReaderWebParagraph[] }): DisplayChapter => {
  const paragraphs = data.paragraphs.map(paragraph => ({ ...paragraph }))
  for (const paragraph of paragraphs) {
    if (/<img\b/.test(paragraph.html) && !/^\s*<img[^>]*src[^>]+>$/.test(paragraph.html)) {
      const document = new DOMParser().parseFromString(paragraph.html, 'text/html')
      for (const image of document.querySelectorAll('img[src]')) {
        image.setAttribute('src', API.getProxyImageUrl(
          store.readingBook.bookUrl, image.getAttribute('src')!, store.config.readWidth,
        ))
      }
      paragraph.html = document.body.innerHTML
    }
  }
  return { index, title: data.title, content: paragraphs }
}

const refreshChapterWindow = (center: number) => {
  if (!infiniteLoading.value || !windowReady || readerDisposed || !catalog.value[center]) return
  if (windowCenter !== center) {
    windowCenter = center
    void updateChapterLayout(items => items)
  }
  const generation = contentGeneration
  for (const [index, request] of chapterPrefetcher.loadWindow(
    store.readingBook.bookUrl, center, catalog.value.length,
  )) {
    if (chapterData.value.some(item => item.index === index) || pendingWindowChapters.has(index)) continue
    const pending = request.then(response => {
      if (readerDisposed || generation !== contentGeneration || !response.data.isSuccess) return
      if (Math.abs(index - windowCenter) > 2) return
      return updateChapterLayout(items => items.some(item => item.index === index)
        ? items : [...items, displayChapter(index, response.data.data)])
    }).catch(() => {}).finally(() => {
      if (pendingWindowChapters.get(index) === pending) pendingWindowChapters.delete(index)
    })
    pendingWindowChapters.set(index, pending)
  }
}

const visibleChapterIndex = () => {
  for (const element of content.value?.querySelectorAll<HTMLElement>('[data-reader-chapter]') ?? []) {
    const rect = element.getBoundingClientRect()
    if (rect.top <= 24 && rect.bottom > 24) return Number(element.dataset.readerChapter)
  }
  // A scrollbar jump can land inside a recycled area. Restore the corresponding window.
  for (const [spacer, before] of [[leadingSpacer.value, true], [trailingSpacer.value, false]] as const) {
    if (!spacer) continue
    const rect = spacer.getBoundingClientRect()
    if (rect.top > 24 || rect.bottom <= 24) continue
    let offset = 24 - rect.top
    const first = chapterData.value[0]?.index ?? windowCenter
    const last = chapterData.value.slice(-1)[0]?.index ?? windowCenter
    for (const [index, height] of [...chapterHeights].sort(([a], [b]) => a - b)) {
      if (before ? index >= first : index <= last) continue
      if (offset < height) return index
      offset -= height
    }
  }
}

const onReaderScroll = () => {
  lastAnchor = captureChapterAnchor(content.value)
  if (scrollFrame || !windowReady) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = 0
    const visible = visibleChapterIndex()
    // Also retry missing chapters while staying in the same chapter. Wheel/touch
    // events reach here even at the page boundary, where no scroll event fires.
    refreshChapterWindow(visible ?? windowCenter)
  })
}

watch(infiniteLoading, enabled => {
  if (!windowReady) return
  if (enabled) refreshChapterWindow(chapterIndex.value)
  else {
    windowCenter = chapterIndex.value
    void updateChapterLayout(items => items)
  }
})
const noPoint = ref(true)
const chapterPrefetcher = createChapterPrefetcher(
  (bookUrl, index) => API.getBookContent(bookUrl, catalog.value[index].index),
  response => response.data.isSuccess,
)
let contentGeneration = 0
let readerDisposed = false
const getContent = (index: number, reloadChapter = true, chapterPos = 0) => {
  if (readerDisposed || !catalog.value[index]) return
  if (reloadChapter) {
    contentGeneration++
    windowReady = false
    windowCenter = index
    lastAnchor = undefined
    pendingWindowChapters.clear()
    chapterResizeObserver?.disconnect()
    chapterHeights.clear()
    spacerHeights.value = { before: 0, after: 0 }
    flushReadSession()
    readSessionStartTime = 0
    //展示进度条
    store.setShowContent(false)
    //强制滚回顶层
    scrollChapterBy(top.value.getBoundingClientRect().top)
    //从目录，按钮切换章节时保存进度 预加载时不保存
    saveReadingBookProgressToBrowser(index, chapterPos)
    chapterData.value = []
  }
  const bookUrl = store.readingBook.bookUrl
  const { title } = catalog.value[index]
  const generation = contentGeneration
  const isCurrent = () => !readerDisposed && generation === contentGeneration

  return loadingWrapper(
    chapterPrefetcher.load(bookUrl, index, catalog.value.length).then(
      async res => {
        if (!isCurrent()) return
        if (res.data.isSuccess) {
          chapterData.value = [displayChapter(index, res.data.data)]
        } else {
          ElMessage({ message: res.data.errorMsg, type: 'error' })
          const content = [{ html: res.data.errorMsg, start: 0, endExclusive: 0 }]
          chapterData.value.push({ index, content, title })
        }
        store.setContentLoading(true)
        noPoint.value = false
        store.setShowContent(true)
        if (!res.data.isSuccess) {
          throw res.data
        }
        if (reloadChapter) await toChapterPos(chapterPos)
        if (!isCurrent()) return
        windowReady = true
        refreshChapterWindow(index)
        beginReadSession()
      },
      err => {
        if (!isCurrent()) return
        const content = [{ html: '获取章节内容失败！', start: 0, endExclusive: 0 }]
        chapterData.value.push({ index, content, title })
        store.setShowContent(true)
        throw err
      },
    ),
  ).catch(() => {
    // Foreground errors are already displayed above; background requests render nothing.
  })
}

// 章节进度跳转和计算
const chapter = ref()
const chapterRef = ref()
const toChapterPos = async (pos: number) => {
  await nextTick()
  const current = chapterRef.value?.find((item: { $props: { chapterIndex: number } }) =>
    item.$props.chapterIndex === chapterIndex.value)
  await current?.scrollToReadedLength(pos)
  lastAnchor = captureChapterAnchor(content.value)
}

// 60秒保存一次进度
const saveBookProgressThrottle = useThrottleFn(
  () => store.saveBookProgress(),
  60000,
)

const onReadedLengthChange = (index: number, pos: number) => {
  if (!windowReady) return
  const visible = visibleChapterIndex()
  if (visible !== undefined && visible !== index) return
  if (index !== windowCenter) refreshChapterWindow(index)
  saveReadingBookProgressToBrowser(index, pos)
  saveBookProgressThrottle()
}

// 文档标题
watchEffect(() => {
  document.title = catalog.value[chapterIndex.value]?.title || document.title
})

// 阅读记录保存浏览器
const saveReadingBookProgressToBrowser = (index: number, pos: number) => {
  // 保存pinia
  chapterIndex.value = index
  chapterPos.value = pos
}

// 进度同步
// 返回导航变化 同步请求会在获取书架前完成

/**
 * VisibilityChange https://developer.mozilla.org/zh-CN/docs/Web/API/Document/visibilitychange_event
 * 监听关闭页面 切换tab 返回桌面 等操作
 * 注意不用监听点击链接导航变化 不对Safari<14.5兼容处理
 **/
const onVisibilityChange = () => {
  const _bookProgress = bookProgress.value
  if (document.visibilityState == 'hidden' && _bookProgress) {
    store.saveBookProgressOnPageHide()
  }
  if (document.visibilityState === 'hidden') {
    flushReadSession()
    readSessionStartTime = 0
    pauseAutoPage(0)
  } else {
    beginReadSession()
    resumeAutoPage()
  }
}
// 定时同步

// 章节切换
const toNextChapter = () => {
  pauseAutoPage()
  store.setContentLoading(true)
  const index = chapterIndex.value + 1
  if (typeof catalog.value[index] !== 'undefined') {
    ElMessage({
      message: '下一章',
      type: 'info',
    })
    getContent(index)
    store.saveBookProgress()
  } else {
    ElMessage({
      message: '本章是最后一章',
      type: 'error',
    })
  }
}
const toPreChapter = () => {
  pauseAutoPage()
  store.setContentLoading(true)
  const index = chapterIndex.value - 1
  if (typeof catalog.value[index] !== 'undefined') {
    ElMessage({
      message: '上一章',
      type: 'info',
    })
    getContent(index)
    store.saveBookProgress()
  } else {
    ElMessage({
      message: '本章是第一章',
      type: 'error',
    })
  }
}

let canJump = true
// 监听方向键
const handleKeyPress = (event: KeyboardEvent) => {
  if (!canJump) return
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    pauseAutoPage()
  }
  switch (event.key) {
    case 'ArrowLeft':
      event.stopPropagation()
      event.preventDefault()
      toPreChapter()
      break
    case 'ArrowRight':
      event.stopPropagation()
      event.preventDefault()
      toNextChapter()
      break
    case 'ArrowUp':
      event.stopPropagation()
      event.preventDefault()
      if (document.documentElement.scrollTop === 0) {
        ElMessage.warning('已到达页面顶部')
      } else {
        canJump = false
        jump(0 - document.documentElement.clientHeight + 100, {
          duration: store.config.jumpDuration,
          callback: () => (canJump = true),
        })
      }
      break
    case 'ArrowDown':
      event.stopPropagation()
      event.preventDefault()
      if (
        document.documentElement.clientHeight +
          document.documentElement.scrollTop ===
        document.documentElement.scrollHeight
      ) {
        ElMessage.warning('已到达页面底部')
      } else {
        canJump = false
        jump(document.documentElement.clientHeight - 100, {
          duration: store.config.jumpDuration,
          callback: () => (canJump = true),
        })
      }
      break
  }
}

// 阻止默认滚动事件
const ignoreKeyPress = (event: KeyboardEvent) => {
  if (event.code === 'Space' || event.key === ' ') {
    const target = event.target
    if (
      event.defaultPrevented || event.ctrlKey || event.metaKey || event.altKey ||
      event.isComposing || popCataVisible.value || readSettingsVisible.value ||
      (target instanceof Element && target.closest(
        'input, textarea, select, button, a[href], [contenteditable]:not([contenteditable="false"]), [role="textbox"], [role="button"], [role="slider"]',
      ))
    ) return
    event.preventDefault()
    event.stopPropagation()
    if (event.repeat || !canJump || !windowReady) return
    pauseAutoPage()
    onReaderScroll()
    const pageHeight = document.documentElement.clientHeight
    const distance = spaceScrollDistance(pageHeight, store.config.fontSize, store.config.spacing.line)
      * (event.shiftKey ? -1 : 1)
    const maxScroll = Math.max(0, document.documentElement.scrollHeight - pageHeight)
    const targetScroll = Math.min(maxScroll, Math.max(0, window.scrollY + distance))
    if (Math.abs(targetScroll - window.scrollY) < 1) return
    canJump = false
    jump(targetScroll - window.scrollY, {
      duration: normalizeSpaceScrollDuration(store.config.spaceScrollDuration),
      callback: () => (canJump = true),
    })
    return
  }
  if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
    event.preventDefault()
    event.stopPropagation()
  }
}

onMounted(async () => {
  chapterResizeObserver = new ResizeObserver(() => {
    if (!windowReady || readerDisposed) return
    restoreChapterAnchor(lastAnchor)
    measureChapters()
    lastAnchor = captureChapterAnchor(content.value)
  })
  window.addEventListener('scroll', onReaderScroll, { passive: true })
  await store.loadWebConfig()
  if (store.config.autoPage) startAutoPage()
  window.addEventListener('wheel', onUserWheel, { passive: true })
  window.addEventListener('touchstart', onUserTouch, { passive: true })
  window.addEventListener('touchmove', onUserTouchMove, { passive: true })
  //获取书籍数据
  const bookUrl = sessionStorage.getItem('bookUrl')
  const name = sessionStorage.getItem('bookName')
  const author = sessionStorage.getItem('bookAuthor')
  const chapterIndex = Number(sessionStorage.getItem('chapterIndex') || 0)
  const chapterPos = Number(sessionStorage.getItem('chapterPos') || 0)
  const isSeachBook = sessionStorage.getItem('isSeachBook') === 'true'
  if (isNullOrBlank(bookUrl) || isNullOrBlank(name) || author === null) {
    ElMessage.warning('书籍信息为空，即将自动返回书架页面...')
    return setTimeout(toShelf, 500)
  }
  const book: typeof store.readingBook = {
    // @ts-expect-error: bookUrl name author is NON_Blank string here
    bookUrl,
    // @ts-expect-error: bookUrl name author is NON_Blank string here
    name,
    author,
    chapterIndex,
    chapterPos,
    isSeachBook,
  }
  onResize()
  window.addEventListener('resize', onResize)
  loadingWrapper(
    store.loadWebCatalog(book).then(chapters => {
      store.setReadingBook(book)
      getContent(chapterIndex, true, chapterPos)
      void store.saveBookProgress()
      window.addEventListener('keyup', handleKeyPress)
      window.addEventListener('keydown', ignoreKeyPress)
      // 兼容Safari < 14
      document.addEventListener('visibilitychange', onVisibilityChange)
      readSessionFlushTimer = window.setInterval(
        () => flushReadSession(),
        READ_SESSION_FLUSH_INTERVAL_MS,
      )
      //监听底部加载
      scrollObserver = new IntersectionObserver(onReachBottom, {
        rootMargin: '-100% 0% 20% 0%',
      })
      if (infiniteLoading.value === true) scrollObserver.observe(loading.value)
      //第二次点击同一本书 页面标题不会变化
      document.title = '...'
      document.title = (name as string) + ' | ' + chapters[chapterIndex].title
    }),
  )
  initXboxGamepad({
    onToggleCatalog: () => {
      popCataVisible.value = !popCataVisible.value
    },
  })
})

onUnmounted(() => {
  readerDisposed = true
  windowReady = false
  chapterResizeObserver?.disconnect()
  window.removeEventListener('scroll', onReaderScroll)
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
  chapterHeights.clear()
  chapterPrefetcher.clear()
  stopReadSession()
  window.removeEventListener('keyup', handleKeyPress)
  window.removeEventListener('keydown', ignoreKeyPress)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('wheel', onUserWheel)
  window.removeEventListener('touchstart', onUserTouch)
  window.removeEventListener('touchmove', onUserTouchMove)
  // 兼容Safari < 14
  document.removeEventListener('visibilitychange', onVisibilityChange)
  stopAutoPage()
  if (autoPageResumeTimer) {
    window.clearTimeout(autoPageResumeTimer)
    autoPageResumeTimer = undefined
  }
  readSettingsVisible.value = false
  popCataVisible.value = false
  scrollObserver?.disconnect()
  scrollObserver = null
})

const addToBookShelfConfirm = async () => {
  const book = store.readingBook
  // 阅读的是搜索的书籍 并未在书架
  if (book.isSeachBook === true) {
    await ElMessageBox.confirm(`是否将《${book.name}》放入书架？`, '放入书架', {
      confirmButtonText: '确认',
      cancelButtonText: '否',
      type: 'info',
      /*
        ElMessageBox.confirm默认在触发hashChange事件时自动关闭
        按下物理返回键时触发hashChange事件
        使用router.push("/")则不会触发hashChange事件
        */
      closeOnHashChange: false,
    })
      .then(() => {
        //选择是，无动作
        isSeachBook.value = false
      })
      .catch(async () => {
        //选择否，删除书籍
        await API.deleteBook(book)
      })
      .finally(() => sessionStorage.removeItem('isSeachBook'))
  }
}
onBeforeRouteLeave(async (to, from, next) => {
  console.log('onBeforeRouteLeave')
  stopReadSession()
  // 弹窗时停止响应按键翻页
  window.removeEventListener('keyup', handleKeyPress)
  await addToBookShelfConfirm()
  next()
})
</script>

<style lang="scss" scoped>
:deep(.pop-setting) {
  margin-left: 68px;
  top: 0;
}

:deep(.pop-cata) {
  margin-left: 10px;
}

.chapter-wrapper {
  overflow-anchor: none;
  padding: 0 4%;

  overflow-x: hidden;

  :deep(.no-point) {
    pointer-events: none;
  }

  .tool-bar {
    position: fixed;
    top: 0;
    left: 50%;
    z-index: 100;

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 58px;
        height: 48px;
        text-align: center;
        padding-top: 12px;
        cursor: pointer;
        outline: none;

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }

        .icon-text {
          font-size: 12px;
        }
      }
    }
  }

  .read-bar {
    position: fixed;
    bottom: 0;
    right: 50%;
    z-index: 100;

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 42px;
        height: 31px;
        padding-top: 12px;
        text-align: center;
        align-items: center;
        cursor: pointer;
        outline: none;
        margin-top: -1px;

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }
      }
    }
  }

  .chapter {
    font-family:
      'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light,
      'Helvetica Neue Light', sans-serif;
    text-align: left;
    padding: 0 65px;
    min-height: 100vh;
    width: 670px;
    margin: 0 auto;

    .content {
      font-size: 18px;
      line-height: 1.8;
      font-family:
        'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light,
        'Helvetica Neue Light', sans-serif;

      .bottom-bar,
      .top-bar {
        height: 64px;
      }
    }
  }
}

.day {
  :deep(.popup) {
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.12),
      0 0 6px rgba(0, 0, 0, 0.04);
  }

  :deep(.tool-icon) {
    border: 1px solid rgba(0, 0, 0, 0.1);
    margin-top: -1px;
    color: #000;

    .icon-text {
      color: rgba(0, 0, 0, 0.4);
    }
  }

  :deep(.chapter) {
    border: 1px solid #d8d8d8;
    color: #262626;
  }
}

.night {
  :deep(.popup) {
    box-shadow:
      0 2px 4px rgba(0, 0, 0, 0.48),
      0 0 6px rgba(0, 0, 0, 0.16);
  }

  :deep(.tool-icon) {
    border: 1px solid #444;
    margin-top: -1px;
    color: #666;

    .icon-text {
      color: #666;
    }
  }

  :deep(.chapter) {
    border: 1px solid #444;
    color: #666;
  }

  :deep(.popper__arrow) {
    background: #666;
  }
}

@media screen and (max-width: 776px) {
  .chapter-wrapper {
    padding: 0;

    .tool-bar {
      left: 0;
      width: 100vw;
      margin-left: 0 !important;

      .tools {
        flex-direction: row;
        justify-content: space-between;

        .tool-icon {
          border: none;
        }
      }
    }

    .read-bar {
      right: 0;
      width: 100vw;
      margin-right: 0 !important;

      .tools {
        flex-direction: row;
        justify-content: space-between;
        padding: 0 15px;

        .tool-icon {
          border: none;
          width: auto;

          .iconfont {
            display: inline-block;
          }
        }
      }
    }

    .chapter {
      width: 100vw !important;
      padding: 0 20px;
      box-sizing: border-box;
      border: none;
    }
  }
}
</style>
