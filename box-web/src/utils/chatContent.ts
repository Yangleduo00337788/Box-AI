export interface ChatImagePart {
  name: string
  url: string
}

export interface ParsedUserContent {
  text: string
  images: ChatImagePart[]
}

const IMAGE_BLOCK =
  /\[图片:\s*([^\]]+)\]\s*\n+((?:https?:\/\/[^\s]+)?\/api\/v1\/public-assets\/[^\s]+)/g

export function parseUserContent(content: string): ParsedUserContent {
  const images: ChatImagePart[] = []
  const text = content
    .replace(IMAGE_BLOCK, (_match, name: string, url: string) => {
      images.push({ name: name.trim(), url: url.trim() })
      return ''
    })
    .replace(/\n{3,}/g, '\n\n')
    .trim()
  return { text, images }
}
