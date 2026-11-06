import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const source = readFileSync(new URL('../src/utils/spaceScrollDuration.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2021, module: ts.ModuleKind.ESNext },
})
const { spaceScrollDistance, normalizeSpaceScrollDuration } =
  await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('space scroll retains one font height and twice the line gap', () => {
  // 20px font, 2px gap (0.1em): retain 20 + 2 * 2 = 24px.
  assert.equal(spaceScrollDistance(800, 20, 0.1), 776)
  assert.equal(spaceScrollDistance(800, 20, 0), 780)
  assert.equal(spaceScrollDistance(800, 24, 0.5), 752)
  assert.equal(spaceScrollDistance(800, 18, 0.8), 753.2)
  assert.equal(spaceScrollDistance(20, 24, 0.5), 0)
})

test('space scroll duration defaults to 1000ms and preserves configured durations', () => {
  assert.equal(normalizeSpaceScrollDuration(undefined), 1000)
  assert.equal(normalizeSpaceScrollDuration(NaN), 1000)
  assert.equal(normalizeSpaceScrollDuration(750), 750)
  assert.equal(normalizeSpaceScrollDuration(0), 100)
  assert.equal(normalizeSpaceScrollDuration(90000), 30000)
})
