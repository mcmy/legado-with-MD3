import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const source = readFileSync(new URL('../src/utils/chapterWindow.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2021, module: ts.ModuleKind.ESNext },
})
const { chapterWindowIndices, chapterSpacerHeights, restoreChapterAnchor } =
  await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('keeps the current chapter and two on each side, without shifting the window at book edges', () => {
  assert.deepEqual(chapterWindowIndices(100, 200), [98, 99, 100, 101, 102])
  assert.deepEqual(chapterWindowIndices(0, 200), [0, 1, 2])
  assert.deepEqual(chapterWindowIndices(199, 200), [197, 198, 199])
  assert.deepEqual(chapterWindowIndices(0, 1), [0])
  assert.deepEqual(chapterWindowIndices(0, 0), [])
})

test('recycled chapter heights preserve document geometry in both reading directions', () => {
  const heights = new Map(Array.from({ length: 105 }, (_, index) => [index, 1200 + index * 0.25]))
  const total = [...heights.values()].reduce((a, b) => a + b, 0)
  for (const center of [2, 50, 100, 99, 50, 2]) {
    const indices = chapterWindowIndices(center, 105)
    const spacers = chapterSpacerHeights(heights, indices[0], indices.at(-1))
    const visibleHeight = indices.reduce((sum, index) => sum + heights.get(index), 0)
    assert.equal(spacers.before + visibleHeight + spacers.after, total)
    assert.equal(spacers.before, [...heights].filter(([index]) => index < indices[0])
      .reduce((sum, [, height]) => sum + height, 0))
  }
})

test('restores the exact anchor offset and temporarily overrides smooth scrolling', () => {
  const values = new Map([['scroll-behavior', ['smooth', 'important']]])
  const scrolls = []
  const originalWindow = globalThis.window
  const originalDocument = globalThis.document
  globalThis.document = { documentElement: { style: {
    getPropertyValue: name => values.get(name)?.[0] ?? '',
    getPropertyPriority: name => values.get(name)?.[1] ?? '',
    setProperty: (name, value, priority) => values.set(name, [value, priority]),
    removeProperty: name => values.delete(name),
  } } }
  globalThis.window = { scrollBy: (x, y) => {
    assert.deepEqual(values.get('scroll-behavior'), ['auto', 'important'])
    scrolls.push(y)
  } }
  try {
    restoreChapterAnchor({ top: -5.25, element: {
      isConnected: true, getBoundingClientRect: () => ({ top: 357.5 }),
    } })
    assert.deepEqual(scrolls, [362.75])
    assert.deepEqual(values.get('scroll-behavior'), ['smooth', 'important'])
    restoreChapterAnchor({ top: 0, element: { isConnected: false } })
    assert.equal(scrolls.length, 1)
  } finally {
    globalThis.window = originalWindow
    globalThis.document = originalDocument
  }
})
