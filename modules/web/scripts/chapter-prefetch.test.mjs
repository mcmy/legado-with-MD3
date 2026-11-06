import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const source = readFileSync(new URL('../src/utils/chapterPrefetch.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2021, module: ts.ModuleKind.ESNext },
})
const { createChapterPrefetcher } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('starts current chapter and next two before any response, reusing pending requests', async () => {
  const calls = []
  const responses = Array.from({ length: 8 }, deferred)
  const loader = createChapterPrefetcher((book, index) => {
    calls.push([book, index])
    return responses[index].promise
  }, () => true)

  const current = loader.load('book', 2, 8)
  assert.deepEqual(calls, [['book', 2], ['book', 3], ['book', 4]])
  assert.equal(loader.load('book', 2, 8), current)
  const next = loader.load('book', 3, 8)
  assert.deepEqual(calls, [['book', 2], ['book', 3], ['book', 4], ['book', 5]])
  responses[3].resolve('next chapter')
  assert.equal(await next, 'next chapter')
  responses[2].resolve('current chapter')
  assert.equal(await current, 'current chapter')
  assert.equal(await loader.load('book', 3, 8), 'next chapter')
  assert.equal(calls.length, 4)
})

test('stops at the last chapter and rejects invalid indices without requesting', async () => {
  const calls = []
  const loader = createChapterPrefetcher(async (_, index) => {
    calls.push(index)
    return index
  }, () => true)
  await loader.load('book', 3, 5)
  await loader.load('book', 4, 5)
  assert.deepEqual(calls, [3, 4])
  await assert.rejects(loader.load('book', 5, 5), RangeError)
  await assert.rejects(loader.load('book', -1, 5), RangeError)
  await assert.rejects(loader.load('book', 0, 0), RangeError)
  assert.deepEqual(calls, [3, 4])
})

test('evicts chapters outside the five-chapter window and isolates books', async () => {
  const calls = []
  const loader = createChapterPrefetcher(async (book, index) => {
    calls.push([book, index])
    return `${book}:${index}`
  }, () => true)
  await loader.load('a', 0, 10)
  await loader.load('a', 5, 10)
  await loader.load('a', 0, 10)
  assert.equal(calls.filter(([book, index]) => book === 'a' && index === 0).length, 2)
  assert.equal(await loader.load('b', 0, 10), 'b:0')
  assert.deepEqual(calls.slice(-3), [['b', 0], ['b', 1], ['b', 2]])
  loader.clear()
  await loader.load('b', 0, 10)
  assert.equal(calls.filter(([book, index]) => book === 'b' && index === 0).length, 2)
})

test('retries failed prefetches, including unsuccessful API responses', async () => {
  const attempts = new Map()
  const loader = createChapterPrefetcher(async (_, index) => {
    const attempt = (attempts.get(index) ?? 0) + 1
    attempts.set(index, attempt)
    if (index === 1 && attempt === 1) throw new Error('network failure')
    return { isSuccess: !(index === 2 && attempt === 1), index }
  }, result => result.isSuccess)
  await loader.load('book', 0, 4)
  assert.equal((await loader.load('book', 1, 4)).isSuccess, true)
  assert.equal((await loader.load('book', 2, 4)).isSuccess, true)
  assert.equal(attempts.get(1), 2)
  assert.equal(attempts.get(2), 2)
})

test('a late failure from an evicted request cannot remove a newer request', async () => {
  const old = deferred()
  let first = true
  let attempts = 0
  const loader = createChapterPrefetcher(async (_, index) => {
    if (index === 1) {
      attempts++
      if (first) { first = false; return old.promise }
    }
    return index
  }, () => true)
  await loader.load('book', 0, 8)
  await loader.load('book', 4, 8)
  await loader.load('book', 1, 8)
  old.reject(new Error('old request failed'))
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(await loader.load('book', 1, 8), 1)
  assert.equal(attempts, 2)
})


test('rolling window fetches two chapters on either side and reuses them in reverse', async () => {
  const calls = []
  const loader = createChapterPrefetcher(async (_, index) => {
    calls.push(index)
    return index
  }, () => true)
  const window = loader.loadWindow('book', 10, 30)
  assert.deepEqual([...window.keys()].sort((a, b) => a - b), [8, 9, 10, 11, 12])
  assert.deepEqual(calls, [10, 11, 12, 8, 9])
  await Promise.all(window.values())
  await Promise.all(loader.loadWindow('book', 11, 30).values())
  assert.deepEqual(calls.slice(5), [13])
  await Promise.all(loader.loadWindow('book', 10, 30).values())
  assert.deepEqual(calls.slice(6), [8])
  const end = loader.loadWindow('book', 29, 30)
  assert.deepEqual([...end.keys()].sort((a, b) => a - b), [27, 28, 29])
  await Promise.all(end.values())
})

test('repeated scroll checks retry only missing chapters and reuse in-flight requests', async () => {
  const calls = new Map()
  const slow = deferred()
  let recover = false
  const loader = createChapterPrefetcher(async (_, index) => {
    calls.set(index, (calls.get(index) ?? 0) + 1)
    if (index === 11 && !recover) return { isSuccess: false }
    if (index === 12) return slow.promise
    return { isSuccess: true }
  }, response => response.isSuccess)
  const check = () => loader.loadWindow('book', 10, 30)
  const first = check()
  await first.get(11)
  for (let i = 0; i < 5; i++) {
    const window = check()
    assert.equal(window.get(12), first.get(12))
    await window.get(11)
  }
  assert.equal(calls.get(11), 6)
  for (const index of [8, 9, 10, 12]) assert.equal(calls.get(index), 1)
  recover = true
  assert.equal((await check().get(11)).isSuccess, true)
  for (let i = 0; i < 20; i++) check()
  assert.equal(calls.get(11), 7)
  assert.equal(calls.get(12), 1)
  slow.resolve({ isSuccess: true })
  await Promise.all(check().values())
})
