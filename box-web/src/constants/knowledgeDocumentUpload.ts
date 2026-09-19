/**
 * Knowledge document upload types. Keep in sync with
 * {@code com.boxai.common.security.FileSafetyPolicy#ALLOWED_EXTENSIONS}
 * (knowledge upload uses {@code FileSafetyPolicy.validate}, not validateImage).
 */
export const KNOWLEDGE_DOCUMENT_EXTENSIONS = [
  'pdf',
  'doc',
  'docx',
  'ppt',
  'pptx',
  'xls',
  'xlsx',
  'txt',
  'md',
  'csv',
  'json',
  'log',
  'png',
  'jpg',
  'jpeg',
  'svg',
] as const

const EXT_ACCEPT = KNOWLEDGE_DOCUMENT_EXTENSIONS.map((ext) => `.${ext}`)

const MIME_ACCEPT = [
  'text/plain',
  'text/markdown',
  'application/json',
  'text/csv',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'image/png',
  'image/jpeg',
  'image/svg+xml',
]

/** HTML file input {@code accept} value. */
export const KNOWLEDGE_DOCUMENT_ACCEPT = [...EXT_ACCEPT, ...MIME_ACCEPT].join(',')

export const KNOWLEDGE_DOCUMENT_UPLOAD_HINT =
  '支持多选上传 TXT、MD、JSON、CSV、LOG、PDF、Word、Excel、PPT、PNG/JPG/SVG 或网页 URL；扫描版 PDF/图片将自动 OCR（需配置平台 OCR/视觉模型）'

const OCR_LIKE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'svg', 'pdf'])

export function knowledgeDocumentMayNeedOcr(fileName: string) {
  const dot = fileName.lastIndexOf('.')
  if (dot < 0) return false
  return OCR_LIKE_EXTENSIONS.has(fileName.substring(dot + 1).toLowerCase())
}
