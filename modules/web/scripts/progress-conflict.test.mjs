import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const compile = path => ts.transpileModule(readFileSync(new URL(path, import.meta.url), 'utf8'), {
  compilerOptions: { target: ts.ScriptTarget.ES2021, module: ts.ModuleKind.CommonJS, esModuleInterop: true },
}).outputText
const progressModule = { exports: {} }
runInNewContext(compile('../src/utils/chapterProgress.ts'), { exports: progressModule.exports })

const conflict = position => ({ data: {
  isSuccess: false, errorMsg: 'conflict', data: {
    name: 'Book', author: 'Author', durChapterIndex: 0, durChapterPos: position,
    durChapterTitle: 'Chapter', durChapterTime: 1,
  },
} })
const storage = () => {
  const values = new Map()
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
}
const createStore = (save, session = storage()) => {
  let options
  const calls = { dialogs: 0, reloads: 0 }
  const exports = {}
  runInNewContext(compile('../src/store/bookStore.ts'), {
    exports, localStorage: storage(), sessionStorage: session, toRaw: value => value,
    location: { hash: '#/chapter', reload: () => { calls.reloads++ } },
    require: name => {
      if (name === 'pinia') return { defineStore: (_, value) => { options = value } }
      if (name === '@api') return { __esModule: true, default: { saveBookProgress: save }, WEB_PROGRESS_CONFLICT: 'conflict' }
      if (name === 'element-plus/es') return { ElMessage: { error: () => {} }, ElMessageBox: { confirm: async () => { calls.dialogs++ } } }
      if (name.includes('spaceScrollDuration')) return { normalizeSpaceScrollDuration: value => value ?? 1000 }
      if (name.includes('chapterProgress')) return progressModule.exports
      throw new Error(name)
    },
  })
  const store = options.state()
  for (const [name, action] of Object.entries(options.actions)) store[name] = action.bind(store)
  for (const [name, getter] of Object.entries(options.getters)) Object.defineProperty(store, name, { get: () => getter(store) })
  store.readingBook = { name: 'Book', author: 'Author', bookUrl: 'book-a', chapterIndex: 0, chapterPos: 10 }
  store.catalog = [{ title: 'Chapter' }]
  return { store, calls }
}

test('accepting remote progress requests in-page restoration without reloading', async () => {
  const { store, calls } = createStore(async () => conflict(150))
  await store.saveBookProgress()
  assert.equal(store.readingBook.chapterPos, 150)
  assert.equal(store.progressRestoreRevision, 1)
  assert.equal(calls.reloads, 0)
})

test('a late conflict behind the current position cannot restore or prompt', async () => {
  let resolve
  const { store, calls } = createStore(() => new Promise(done => { resolve = done }))
  const pending = store.saveBookProgress()
  store.readingBook.chapterPos = 200
  resolve(conflict(150))
  await pending
  assert.equal(calls.dialogs, 0)
  assert.equal(store.readingBook.chapterPos, 200)
})

test('a response for a previous book cannot change the newly opened book', async () => {
  let resolve
  const { store, calls } = createStore(() => new Promise(done => { resolve = done }))
  const pending = store.saveBookProgress()
  store.readingBook = { ...store.readingBook, bookUrl: 'book-b', chapterPos: 20 }
  resolve(conflict(150))
  await pending
  assert.equal(calls.dialogs, 0)
  assert.equal(store.readingBook.chapterPos, 20)
})

test('paragraph observation preserves an exact restored offset but permits moving to another paragraph', () => {
  const { positionWithinVisibleParagraph } = progressModule.exports
  assert.equal(positionWithinVisibleParagraph(150, 100, 200), 150)
  assert.equal(positionWithinVisibleParagraph(150, 200, 300), 200)
  assert.equal(positionWithinVisibleParagraph(150, 0, 100), 0)
  assert.equal(positionWithinVisibleParagraph(200, 100, 200), 200)
})


test('accepted remote progress does not prompt again after refresh or a clamped short page', async () => {
  const session = storage()
  const first = createStore(async () => conflict(150), session)
  await first.store.saveBookProgress()
  assert.equal(first.calls.dialogs, 1)
  // A reload or viewport clamping reports an earlier paragraph again.
  const refreshed = createStore(async () => conflict(150), session)
  await refreshed.store.saveBookProgress()
  assert.equal(refreshed.calls.dialogs, 0)
  assert.equal(refreshed.calls.reloads, 0)
})

test('a genuinely newer remote position still offers restoration after an earlier acceptance', async () => {
  const session = storage()
  await createStore(async () => conflict(150), session).store.saveBookProgress()
  const newer = createStore(async () => conflict(250), session)
  await newer.store.saveBookProgress()
  assert.equal(newer.calls.dialogs, 1)
  assert.equal(newer.store.readingBook.chapterPos, 250)
})
