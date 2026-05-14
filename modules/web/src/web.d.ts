export type webReadConfig = {
  theme: number
  font: number
  fontSize: number
  readWidth: number
  infiniteLoading: boolean
  customFontName: string
  jumpDuration: number
  customTheme: {
    enabled: boolean
    textColor: string
    bodyBgColor: string
    contentBgColor: string
    popupBgColor: string
  }
  spacing: {
    paragraph: number
    line: number
    letter: number
  }
}
