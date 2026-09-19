export type ModelCapabilityKind =
  | 'text'
  | 'vision'
  | 'embedding'
  | 'rerank'
  | 'image'
  | 'audio'
  | 'video'
  | 'ocr'

const RULES: { kind: ModelCapabilityKind; test: RegExp }[] = [
  { kind: 'embedding', test: /embed|embedding|\bbge[-._/]|\be5[-._]|gte[-._]/ },
  { kind: 'rerank', test: /rerank|reranker/ },
  { kind: 'ocr', test: /\bocr\b|paddleocr/ },
  { kind: 'audio', test: /\btts\b|\basr\b|whisper|cosyvoice|sensevoice|funaudio|speech|voice/ },
  { kind: 'video', test: /\bvideo\b|\bt2v\b|\bi2v\b|kling|wanx|wan2|\bsvd\b/ },
  { kind: 'image', test: /kolors|\bflux\b|stable[\s-]?diffusion|\bsdxl\b|\bsd3\b|dall-?e|imagen\b|text-to-image|\bt2i\b|\bi2i\b|image-edit|ernie-image|z-image|qwen-image|[-_/](?:text-to-)?image(?:[-_/]|$)/ },
  { kind: 'vision', test: /(\bvl\b|vision|\bomni\b|gpt-4o|gpt-4\.1|gpt-5|internvl|qwen3-vl|qwen2\.5-vl|qwen2-vl|glm-4v|glm-4\.5v|4\.5v\b|\b4v\b|kimi-vl|step-1v|step-1o|\bqvq\b|gemini|claude-3|claude-4|gpt-4\.5|多模态)/ },
]

export function classifyModelChatKind(modelCode?: string, modelName?: string, description?: string): ModelCapabilityKind {
  const text = `${modelCode || ''} ${modelName || ''} ${description || ''}`.toLowerCase()
  if (!text.trim()) return 'text'
  return RULES.find((rule) => rule.test.test(text))?.kind ?? 'text'
}

export function isOcrOrVisionModel(modelCode?: string, modelName?: string, description?: string) {
  const kind = classifyModelChatKind(modelCode, modelName, description)
  return kind === 'ocr' || kind === 'vision'
}
