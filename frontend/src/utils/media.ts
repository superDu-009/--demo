export const normalizeMediaUrl = (value?: string | null) => {
  const raw = value?.trim()
  if (!raw) return ''

  try {
    const url = new URL(raw)
    url.pathname = url.pathname
      .split('/')
      .map(segment => encodeURIComponent(decodeURIComponent(segment)))
      .join('/')
    return url.toString()
  } catch {
    return raw
  }
}

export const isImageUrl = (value?: string | null) => {
  const normalized = normalizeMediaUrl(value)
  if (!normalized) return false

  try {
    return /\.(png|jpe?g|gif|webp)$/i.test(new URL(normalized).pathname)
  } catch {
    return /\.(png|jpe?g|gif|webp)(?:[?#].*)?$/i.test(normalized)
  }
}
