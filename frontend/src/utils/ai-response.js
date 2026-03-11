export const sanitizeAiText = (value) => {
  const text = String(value ?? '').replace(/\r\n/g, '\n')

  return text
    .replace(/^#{1,6}\s*/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/^\s*[-*]\s+/gm, '- ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

export const buildAiResponseText = (payload) => {
  if (!payload) return '无响应'
  if (typeof payload === 'string') return sanitizeAiText(payload)

  const parts = []

  if (payload.summary) parts.push(payload.summary)
  if (Array.isArray(payload.keyPoints) && payload.keyPoints.length) {
    parts.push(payload.keyPoints.map((item) => `- ${sanitizeAiText(item)}`).join('\n'))
  }
  if (payload.contextSource) {
    parts.push(`来源：${sanitizeAiText(payload.contextSource)}`)
  }
  if (payload.response) parts.push(payload.response)
  if (Array.isArray(payload.suggestions) && payload.suggestions.length) {
    parts.push(payload.suggestions.map((item) => `- ${sanitizeAiText(item)}`).join('\n'))
  }
  if (payload.error) parts.push(payload.error)

  const merged = parts.filter(Boolean).join('\n\n')
  return sanitizeAiText(merged || JSON.stringify(payload, null, 2))
}
