/** Keep at most two chapters on either side, including requests still in flight. */
export const createChapterPrefetcher = <T>(
  fetchChapter: (bookUrl: string, index: number) => Promise<T>,
  isCacheable: (result: T) => boolean,
) => {
  const requests = new Map<number, Promise<T>>()
  let cachedBookUrl: string | undefined

  const request = (bookUrl: string, index: number): Promise<T> => {
    const existing = requests.get(index)
    if (existing) return existing

    const evict = () => {
      if (requests.get(index) === pending) requests.delete(index)
    }
    const pending = fetchChapter(bookUrl, index).then(
      result => {
        if (!isCacheable(result)) evict()
        return result
      },
      error => {
        evict()
        throw error
      },
    )
    requests.set(index, pending)
    return pending
  }

  return {
    load(bookUrl: string, index: number, chapterCount: number): Promise<T> {
      if (!Number.isInteger(index) || index < 0 || index >= chapterCount) {
        return Promise.reject(new RangeError('Chapter index is out of bounds'))
      }
      if (cachedBookUrl !== bookUrl) {
        requests.clear()
        cachedBookUrl = bookUrl
      }
      const end = Math.min(index + 2, chapterCount - 1)
      for (const cachedIndex of requests.keys()) {
        if (cachedIndex < index - 2 || cachedIndex > end) requests.delete(cachedIndex)
      }
      const current = request(bookUrl, index)
      for (let next = index + 1; next <= end; next++) {
        // A failed prefetch is retried when the reader actually opens the chapter.
        void request(bookUrl, next).catch(() => {})
      }
      return current
    },
    loadWindow(bookUrl: string, index: number, chapterCount: number) {
      const current = this.load(bookUrl, index, chapterCount)
      const window = new Map<number, Promise<T>>([[index, current]])
      for (let next = index + 1; next <= Math.min(index + 2, chapterCount - 1); next++) {
        window.set(next, request(bookUrl, next))
      }
      for (let previous = Math.max(0, index - 2); previous < index; previous++) {
        const pending = request(bookUrl, previous)
        void pending.catch(() => {})
        window.set(previous, pending)
      }
      return window
    },
    clear() {
      requests.clear()
      cachedBookUrl = undefined
    },
  }
}
