export interface BoxClientOptions {
  baseUrl: string
  apiKey: string
}

export interface BoxChatHistoryItem {
  role: 'USER' | 'ASSISTANT' | string
  content: string
}

export interface BoxCitation {
  index: number
  chunkId?: number
  documentId?: number
  documentName?: string
  content?: string
  score?: number
}

export interface BoxToolEventPayload {
  toolKey: string
  arguments?: Record<string, unknown>
  output?: string
  status?: string
}

export interface BoxToolConfirmPayload {
  confirmationToken: string
  toolKey: string
  toolName?: string
  arguments?: Record<string, unknown>
}

export interface BoxChatOptions {
  stream?: boolean
  onDelta?: (chunk: string) => void
  onCitations?: (citations: BoxCitation[]) => void
  onToolStart?: (payload: BoxToolEventPayload) => void
  onToolDelta?: (payload: BoxToolEventPayload) => void
  onToolEnd?: (payload: BoxToolEventPayload) => void
  onToolConfirm?: (payload: BoxToolConfirmPayload) => void
  history?: BoxChatHistoryItem[]
  toolConfirmationToken?: string
}

export interface BoxChatResult {
  content: string
  citations: BoxCitation[]
  executionId?: number
}

export declare class BoxClient {
  constructor(options: BoxClientOptions)
  chat(agentId: number, message: string, options?: BoxChatOptions): Promise<string | BoxChatResult>
  embedConfig(agentId: number): Promise<Record<string, unknown>>
}
