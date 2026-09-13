import baai from '@lobehub/icons-static-svg/icons/baai.svg?url'
import baichuan from '@lobehub/icons-static-svg/icons/baichuan-color.svg?url'
import bytedance from '@lobehub/icons-static-svg/icons/bytedance-color.svg?url'
import claude from '@lobehub/icons-static-svg/icons/claude-color.svg?url'
import deepseek from '@lobehub/icons-static-svg/icons/deepseek-color.svg?url'
import doubao from '@lobehub/icons-static-svg/icons/doubao-color.svg?url'
import gemini from '@lobehub/icons-static-svg/icons/gemini-color.svg?url'
import grok from '@lobehub/icons-static-svg/icons/grok.svg?url'
import hunyuan from '@lobehub/icons-static-svg/icons/hunyuan-color.svg?url'
import internlm from '@lobehub/icons-static-svg/icons/internlm-color.svg?url'
import kimi from '@lobehub/icons-static-svg/icons/kimi-color.svg?url'
import kwaipilot from '@lobehub/icons-static-svg/icons/kwaipilot-color.svg?url'
import longcat from '@lobehub/icons-static-svg/icons/longcat-color.svg?url'
import meta from '@lobehub/icons-static-svg/icons/meta-color.svg?url'
import minimax from '@lobehub/icons-static-svg/icons/minimax-color.svg?url'
import mistral from '@lobehub/icons-static-svg/icons/mistral-color.svg?url'
import openai from '@lobehub/icons-static-svg/icons/openai.svg?url'
import qwen from '@lobehub/icons-static-svg/icons/qwen-color.svg?url'
import siliconcloud from '@lobehub/icons-static-svg/icons/siliconcloud-color.svg?url'
import spark from '@lobehub/icons-static-svg/icons/spark-color.svg?url'
import stepfun from '@lobehub/icons-static-svg/icons/stepfun-color.svg?url'
import yi from '@lobehub/icons-static-svg/icons/yi-color.svg?url'
import zai from '@lobehub/icons-static-svg/icons/zai.svg?url'
import zhipu from '@lobehub/icons-static-svg/icons/zhipu-color.svg?url'

const BRANDS: { test: RegExp; src: string }[] = [
  { test: /zai[-_/]?org|\bz\.ai\b/, src: zai },
  { test: /zhipu|chatglm|\bglm[-._/]|\bglm\b|智谱|qingyan/, src: zhipu },
  { test: /tencent|hunyuan|\bhy[-._]?\d|混元|yuanbao/, src: hunyuan },
  { test: /qwen|tongyi|alibaba|dashscope|千问/, src: qwen },
  { test: /deepseek/, src: deepseek },
  { test: /moonshot|\bkimi\b/, src: kimi },
  { test: /stepfun|step[-._]?\d/, src: stepfun },
  { test: /doubao|豆包/, src: doubao },
  { test: /bytedance|volcengine|volc|字节/, src: bytedance },
  { test: /longcat|meituan|美团/, src: longcat },
  { test: /minimax/, src: minimax },
  { test: /\bbaai\b|\bbge[-._]/, src: baai },
  { test: /kwai|kolors|快手/, src: kwaipilot },
  { test: /internlm|书生/, src: internlm },
  { test: /\byi[-._/]|01[-.]ai|零一/, src: yi },
  { test: /baichuan|百川/, src: baichuan },
  { test: /spark|iflytek|讯飞/, src: spark },
  { test: /openai|\bgpt[-._]|\bo[1-4][-._]/, src: openai },
  { test: /claude|anthropic/, src: claude },
  { test: /gemini|google/, src: gemini },
  { test: /llama|meta-llama|\bmeta\b/, src: meta },
  { test: /mistral/, src: mistral },
  { test: /\bgrok\b|\bxai\b/, src: grok },
  { test: /silicon|硅基/, src: siliconcloud },
]

export function resolveModelBrandLogo(modelCode?: string, modelName?: string, providerName?: string) {
  const haystack = `${modelCode || ''} ${modelName || ''} ${providerName || ''}`.toLowerCase()
  return BRANDS.find((item) => item.test.test(haystack))?.src
}
