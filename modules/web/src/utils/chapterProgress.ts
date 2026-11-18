/** Offsets are supplied by the server in the Android reader's UTF-16 body space. */
export const paragraphAtPosition = (
  paragraphs: ReadonlyArray<{ start: number; endExclusive: number }>,
  position: number,
): number => {
  if (paragraphs.length === 0) return -1
  const index = paragraphs.findIndex(paragraph => paragraph.endExclusive > position)
  return index < 0 ? paragraphs.length - 1 : index
}

/** Keep Android's exact UTF-16 offset while Web still displays that paragraph. */
export const positionWithinVisibleParagraph = (
  current: number,
  start: number,
  endExclusive: number,
): number => current >= start && current <= endExclusive ? current : start

export const compareBookProgress = (
  left: { durChapterIndex: number; durChapterPos: number },
  right: { durChapterIndex: number; durChapterPos: number },
): number => left.durChapterIndex - right.durChapterIndex || left.durChapterPos - right.durChapterPos
