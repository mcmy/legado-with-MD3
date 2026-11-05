export const chapterWindowIndices = (current: number, total: number) => {
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, i) => start + i)
}

export const chapterSpacerHeights = (
  heights: ReadonlyMap<number, number>,
  first: number,
  last: number,
) => {
  let before = 0
  let after = 0
  for (const [index, height] of heights) {
    if (index < first) before += height
    if (index > last) after += height
  }
  return { before, after }
}

export type ScrollAnchor = { element: HTMLElement; top: number }

export const captureChapterAnchor = (container?: HTMLElement): ScrollAnchor | undefined => {
  if (!container) return
  for (const chapter of container.querySelectorAll<HTMLElement>('[data-reader-chapter]')) {
    const rect = chapter.getBoundingClientRect()
    if (rect.bottom <= 0 || rect.top >= window.innerHeight) continue
    for (const element of chapter.querySelectorAll<HTMLElement>('[data-chapterpos]')) {
      const rect = element.getBoundingClientRect()
      if (rect.bottom > 0 && rect.top < window.innerHeight) {
        return { element, top: rect.top }
      }
    }
  }
}

/** Correct before paint, bypassing any global smooth-scroll CSS. */
export const scrollChapterBy = (delta: number) => {
  if (Math.abs(delta) < 0.01) return
  const root = document.documentElement
  const value = root.style.getPropertyValue('scroll-behavior')
  const priority = root.style.getPropertyPriority('scroll-behavior')
  root.style.setProperty('scroll-behavior', 'auto', 'important')
  window.scrollBy(0, delta)
  if (value) root.style.setProperty('scroll-behavior', value, priority)
  else root.style.removeProperty('scroll-behavior')
}

export const restoreChapterAnchor = (anchor?: ScrollAnchor) => {
  if (!anchor?.element.isConnected) return
  scrollChapterBy(anchor.element.getBoundingClientRect().top - anchor.top)
}
