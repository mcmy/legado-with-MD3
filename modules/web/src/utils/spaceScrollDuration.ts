export const normalizeSpaceScrollDuration = (value: unknown): number => {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 1000
  return Math.round(Math.min(30_000, Math.max(100, value)))
}

export const spaceScrollDistance = (viewportHeight: number, fontSize: number, lineSpacing: number) => {
  // Paragraph CSS uses line-height: 1 + lineSpacing; the gap is fontSize * lineSpacing.
  const lineGap = fontSize * lineSpacing
  return Math.max(0, viewportHeight - fontSize - 2 * lineGap)
}
