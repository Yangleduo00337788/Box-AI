export interface BoxClientOptions {
  baseUrl: string
  apiKey: string
}

export interface BoxChatOptions {
  stream?: boolean
  onDelta?: (chunk: string) => void
}

export declare class BoxClient {
  constructor(options: BoxClientOptions)
  chat(agentId: number, message: string, options?: BoxChatOptions): Promise<string>
  embedConfig(agentId: number): Promise<Record<string, unknown>>
}
