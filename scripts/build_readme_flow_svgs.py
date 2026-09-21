# -*- coding: utf-8 -*-
"""Generate UTF-8 flow SVG assets for README."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "readme"

FONT = 'system-ui, "PingFang SC", "Microsoft YaHei", sans-serif'
FONT_ESC = "system-ui, &quot;PingFang SC&quot;, &quot;Microsoft YaHei&quot;, sans-serif"

files = {
    "flow-rag.svg": f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="320" viewBox="0 0 1200 320" role="img" aria-labelledby="ragTitle">
  <title id="ragTitle">Box RAG \u77e5\u8bc6\u7ba1\u7ebf</title>
  <rect width="1200" height="320" rx="20" fill="#fafafa"/>
  <text x="60" y="48" fill="#222222" font-family="{FONT_ESC}" font-size="24" font-weight="700">RAG \u77e5\u8bc6\u7ba1\u7ebf</text>
  <text x="60" y="76" fill="#777777" font-family="{FONT_ESC}" font-size="16">\u4e1a\u52a1\u5143\u6570\u636e\u4e0e\u6587\u4ef6\u5b58\u50a8\u5206\u79bb\uff0c\u641c\u7d22\u4e0e\u5411\u91cf\u7531 Elasticsearch \u627f\u62c5</text>
  <text x="60" y="118" fill="#8b8b8b" font-family="Consolas, Monaco, monospace" font-size="13">INGEST</text>
  <g transform="translate(60 130)">
    <rect x="0" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="65" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u6587\u6863\u4e0a\u4f20</text>
    <text x="65" y="42" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">PDF / DOCX / MD</text>
    <path d="M138 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="166,28 158,24 158,32" fill="#c5c5c5"/>
    <rect x="174" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="239" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">MinIO</text>
    <text x="239" y="42" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u539f\u59cb\u6587\u4ef6\u5bf9\u8c61</text>
    <path d="M312 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="340,28 332,24 332,32" fill="#c5c5c5"/>
    <rect x="348" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="413" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u89e3\u6790\u5206\u5757</text>
    <text x="413" y="42" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Chunk</text>
    <path d="M486 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="514,28 506,24 506,32" fill="#c5c5c5"/>
    <rect x="522" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="587" y="24" fill="#222222" text-anchor="middle" font-family="Consolas, monospace" font-size="14" font-weight="600">Embedding</text>
    <text x="587" y="42" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u5411\u91cf\u751f\u6210</text>
    <path d="M660 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="688,28 680,24 680,32" fill="#c5c5c5"/>
    <rect x="696" y="0" width="150" height="56" rx="12" fill="#222222"/>
    <text x="771" y="24" fill="#ffffff" text-anchor="middle" font-family="Consolas, monospace" font-size="14" font-weight="600">Elasticsearch</text>
    <text x="771" y="42" fill="#cbcbcb" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u7d22\u5f15\u4e0e\u5411\u91cf</text>
  </g>
  <text x="60" y="228" fill="#8b8b8b" font-family="Consolas, Monaco, monospace" font-size="13">QUERY</text>
  <g transform="translate(60 240)">
    <rect x="0" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="65" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u7528\u6237\u95ee\u9898</text>
    <text x="65" y="42" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Query</text>
    <path d="M138 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="166,28 158,24 158,32" fill="#c5c5c5"/>
    <rect x="174" y="0" width="150" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="249" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u6df7\u5408\u641c\u7d22</text>
    <text x="249" y="42" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u5411\u91cf + \u5173\u952e\u8bcd</text>
    <path d="M332 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="360,28 352,24 352,32" fill="#c5c5c5"/>
    <rect x="368" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="433" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u91cd\u6392\u5e8f</text>
    <text x="433" y="42" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Rerank</text>
    <path d="M506 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="534,28 526,24 526,32" fill="#c5c5c5"/>
    <rect x="542" y="0" width="130" height="56" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="607" y="24" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u4e0a\u4e0b\u6587\u6ce8\u5165</text>
    <text x="607" y="42" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Citation</text>
    <path d="M680 28h28" stroke="#c5c5c5" stroke-width="2"/><polygon points="708,28 700,24 700,32" fill="#c5c5c5"/>
    <rect x="716" y="0" width="130" height="56" rx="12" fill="#222222"/>
    <text x="781" y="32" fill="#ffffff" text-anchor="middle" font-family="Consolas, monospace" font-size="14" font-weight="600">LLM</text>
  </g>
</svg>
""",
}

# architecture refresh with unicode escapes - write architecture via script too in same run
files["architecture.svg"] = open(ROOT / "assets/readme/architecture.svg", encoding="utf-8").read() if (ROOT / "assets/readme/architecture.svg").exists() else ""

# Simpler: only rewrite the three broken flow files + lifecycle + workflow in one dict

files["flow-workflow.svg"] = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="300" viewBox="0 0 1200 300" role="img" aria-labelledby="wfTitle">
  <title id="wfTitle">Box Workflow \u6267\u884c\u6d41\u7a0b</title>
  <rect width="1200" height="300" rx="20" fill="#fafafa"/>
  <text x="60" y="48" fill="#222222" font-family="{FONT_ESC}" font-size="24" font-weight="700">Workflow \u6267\u884c\u5f15\u64ce</text>
  <text x="60" y="76" fill="#777777" font-family="{FONT_ESC}" font-size="16">\u5b9a\u4e49\u4e0e\u8fd0\u884c\u65f6\u5206\u79bb\uff0c\u8282\u70b9\u6267\u884c\u91c7\u7528 Strategy + Registry \u6269\u5c55</text>
  <g transform="translate(60 110)">
    <rect x="0" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="60" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u56fe\u6821\u9a8c</text>
    <text x="60" y="46" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Start / Output</text>
    <path d="M128 32h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="160,32 152,28 152,36" fill="#c5c5c5"/>
    <rect x="168" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="228" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u521b\u5efa\u6267\u884c</text>
    <text x="228" y="46" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Execution</text>
    <path d="M296 32h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="328,32 320,28 320,36" fill="#c5c5c5"/>
    <rect x="336" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="396" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u5b9a\u4f4d\u8d77\u70b9</text>
    <text x="396" y="46" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Start Node</text>
    <path d="M464 32h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="496,32 488,28 488,36" fill="#c5c5c5"/>
    <rect x="504" y="0" width="140" height="64" rx="12" fill="#222222"/>
    <text x="574" y="28" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u8282\u70b9\u6267\u884c\u5668</text>
    <text x="574" y="46" fill="#cbcbcb" text-anchor="middle" font-family="Consolas, monospace" font-size="11">LLM / HTTP / Code</text>
    <path d="M652 32h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="684,32 676,28 676,36" fill="#c5c5c5"/>
    <rect x="692" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="752" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u8def\u7531\u8fb9</text>
    <text x="752" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u6761\u4ef6\u5206\u652f</text>
    <path d="M820 32h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="852,32 844,28 844,36" fill="#c5c5c5"/>
    <rect x="860" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="920" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u8f93\u51fa\u7ed3\u679c</text>
    <text x="920" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">Trace \u843d\u5e93</text>
  </g>
  <g transform="translate(60 200)">
    <rect x="0" y="0" width="1080" height="72" rx="14" fill="#ffffff" stroke="#e7e7e7"/>
    <text x="24" y="28" fill="#8b8b8b" font-family="Consolas, Monaco, monospace" font-size="12">NODE TYPES</text>
    <text x="24" y="52" fill="#484848" font-family="{FONT_ESC}" font-size="14">Start / Input / Output / LLM / Agent / Knowledge / HTTP / Condition / Switch / Loop / Code / Variable / Template</text>
  </g>
</svg>
"""

files["flow-lifecycle.svg"] = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="280" viewBox="0 0 1200 280" role="img" aria-labelledby="lcTitle">
  <title id="lcTitle">Box Agent \u914d\u7f6e\u4e0e\u53d1\u5e03\u751f\u547d\u5468\u671f</title>
  <rect width="1200" height="280" rx="20" fill="#fafafa"/>
  <text x="60" y="48" fill="#222222" font-family="{FONT_ESC}" font-size="24" font-weight="700">Agent \u914d\u7f6e\u4e0e\u53d1\u5e03\u751f\u547d\u5468\u671f</text>
  <text x="60" y="76" fill="#777777" font-family="{FONT_ESC}" font-size="16">\u751f\u4ea7\u73af\u5883\u4ec5\u8fd0\u884c Published Version\uff0c\u8349\u7a3f\u4e0e\u5df2\u53d1\u5e03\u914d\u7f6e\u4e25\u683c\u9694\u79bb</text>
  <g transform="translate(60 110)">
    <rect x="0" y="0" width="140" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="70" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u8349\u7a3f\u7f16\u8f91</text>
    <text x="70" y="46" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Builder</text>
    <path d="M148 32h36" stroke="#c5c5c5" stroke-width="2"/><polygon points="184,32 176,28 176,36" fill="#c5c5c5"/>
    <rect x="192" y="0" width="140" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="262" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u914d\u7f6e\u6821\u9a8c</text>
    <text x="262" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u6a21\u578b / \u5de5\u5177 / \u77e5\u8bc6\u5e93</text>
    <path d="M340 32h36" stroke="#c5c5c5" stroke-width="2"/><polygon points="376,32 368,28 368,36" fill="#c5c5c5"/>
    <rect x="384" y="0" width="140" height="64" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="454" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u751f\u6210\u7248\u672c</text>
    <text x="454" y="46" fill="#8b8b8b" text-anchor="middle" font-family="Consolas, monospace" font-size="11">AgentVersion</text>
    <path d="M532 32h36" stroke="#c5c5c5" stroke-width="2"/><polygon points="568,32 560,28 560,36" fill="#c5c5c5"/>
    <rect x="576" y="0" width="140" height="64" rx="12" fill="#222222"/>
    <text x="646" y="28" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u53d1\u5e03\u4e0a\u7ebf</text>
    <text x="646" y="46" fill="#cbcbcb" text-anchor="middle" font-family="Consolas, monospace" font-size="11">Published</text>
    <path d="M724 32h36" stroke="#c5c5c5" stroke-width="2"/><polygon points="760,32 752,28 752,36" fill="#c5c5c5"/>
    <rect x="768" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="828" y="28" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">Web Chat</text>
    <text x="828" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u5bf9\u8bdd\u5de5\u4f5c\u53f0</text>
    <rect x="904" y="0" width="120" height="64" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="964" y="28" fill="#222222" text-anchor="middle" font-family="Consolas, monospace" font-size="14" font-weight="600">Open API</text>
    <text x="964" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">API Key</text>
    <rect x="1040" y="0" width="100" height="64" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="1090" y="28" fill="#222222" text-anchor="middle" font-family="Consolas, monospace" font-size="14" font-weight="600">Embed</text>
    <text x="1090" y="46" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u5d4c\u5165\u7ad9\u70b9</text>
    <path d="M888 32h12" stroke="#c5c5c5" stroke-width="2"/>
    <path d="M1024 32h12" stroke="#c5c5c5" stroke-width="2"/>
  </g>
  <text x="60" y="248" fill="#8b8b8b" font-family="{FONT_ESC}" font-size="14">\u8fd0\u884c\u65f6\uff1abox-agent\uff08AgentChatExecutor\uff09\u8d1f\u8d23\u5bf9\u8bdd\uff1bbox-runtime\uff08WorkflowExecutor\uff09\u8d1f\u8d23\u5de5\u4f5c\u6d41\u3002</text>
</svg>
"""

files["flow-product.svg"] = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="480" viewBox="0 0 1200 480" role="img" aria-labelledby="prodTitle">
  <title id="prodTitle">Box \u4ea7\u54c1\u80fd\u529b\u5168\u666f</title>
  <rect width="1200" height="480" rx="20" fill="#fafafa"/>
  <text x="60" y="48" fill="#222222" font-family="{FONT_ESC}" font-size="24" font-weight="700">\u4ea7\u54c1\u80fd\u529b\u5168\u666f</text>
  <text x="60" y="76" fill="#777777" font-family="{FONT_ESC}" font-size="16">C \u7aef\u5de5\u4f5c\u53f0 + \u5e73\u53f0\u7ba1\u7406 + \u5f00\u653e\u96c6\u6210\uff0c\u8986\u76d6 Agent \u5168\u751f\u547d\u5468\u671f</text>

  <g transform="translate(60 110)">
    <rect x="0" y="0" width="340" height="320" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="24" y="32" fill="#8b8b8b" font-family="Consolas, monospace" font-size="12">BOX-WEB</text>
    <text x="24" y="58" fill="#222222" font-family="{FONT_ESC}" font-size="18" font-weight="600">C \u7aef\u5de5\u4f5c\u53f0</text>
    <rect x="24" y="78" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="104" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u5bf9\u8bdd\u5de5\u4f5c\u53f0 /chat</text>
    <rect x="176" y="78" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="104" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">Agent Builder</text>
    <rect x="24" y="134" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="160" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u5de5\u4f5c\u6d41\u7f16\u6392\u5668</text>
    <rect x="176" y="134" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="160" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u63d2\u4ef6\u5e02\u573a</text>
    <rect x="24" y="190" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="216" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u77e5\u8bc6\u5e93 / \u5de5\u5177</text>
    <rect x="176" y="190" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="216" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u6267\u884c\u4e0e\u8c03\u8bd5</text>
    <rect x="24" y="246" width="292" height="52" rx="10" fill="#222222"/><text x="170" y="276" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">\u8bbe\u7f6e\uff1aRBAC / API Key / \u5957\u9910\u4e0e\u5ba1\u8ba1</text>
  </g>

  <g transform="translate(430 110)">
    <rect x="0" y="0" width="340" height="320" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="24" y="32" fill="#8b8b8b" font-family="Consolas, monospace" font-size="12">BOX-ADMIN-WEB</text>
    <text x="24" y="58" fill="#222222" font-family="{FONT_ESC}" font-size="18" font-weight="600">\u5e73\u53f0\u7ba1\u7406</text>
    <rect x="24" y="78" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="104" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u79df\u6237\u4e0e\u6210\u5458</text>
    <rect x="176" y="78" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="104" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u5957\u9910\u4e0e\u8ba1\u8d39</text>
    <rect x="24" y="134" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="160" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u6a21\u677f\u4e0e\u63d2\u4ef6\u5ba1\u6838</text>
    <rect x="176" y="134" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="160" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u5e73\u53f0\u5206\u6790</text>
    <rect x="24" y="190" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="94" y="216" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u8fd0\u8425\u4f4d\u914d\u7f6e</text>
    <rect x="176" y="190" width="140" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="246" y="216" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u5168\u5e73\u53f0\u5ba1\u8ba1</text>
    <rect x="24" y="246" width="292" height="52" rx="10" fill="#222222"/><text x="170" y="276" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">SUPER_ADMIN / OPS / FINANCE / CONTENT</text>
  </g>

  <g transform="translate(800 110)">
    <rect x="0" y="0" width="340" height="320" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="24" y="32" fill="#8b8b8b" font-family="Consolas, monospace" font-size="12">OPEN DELIVERY</text>
    <text x="24" y="58" fill="#222222" font-family="{FONT_ESC}" font-size="18" font-weight="600">\u5f00\u653e\u4e0e\u96c6\u6210</text>
    <rect x="24" y="78" width="292" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="170" y="104" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">REST API /api/v1</text>
    <rect x="24" y="134" width="292" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="170" y="160" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">SSE \u6d41\u5f0f\u5bf9\u8bdd</text>
    <rect x="24" y="190" width="292" height="44" rx="10" fill="#f3f3f3" stroke="#e7e7e7"/><text x="170" y="216" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">API Key + box-sdk (JS / Python)</text>
    <rect x="24" y="246" width="292" height="52" rx="10" fill="#48c79c" fill-opacity="0.12" stroke="#48c79c" stroke-width="2"/><text x="170" y="276" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">Embed Web Chat + \u81ea\u5b9a\u4e49\u57df\u540d</text>
  </g>
</svg>
"""

files["flow-agent-chat.svg"] = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="360" viewBox="0 0 1200 360" role="img" aria-labelledby="chatTitle">
  <title id="chatTitle">Agent \u5bf9\u8bdd\u65f6\u5e8f</title>
  <rect width="1200" height="360" rx="20" fill="#fafafa"/>
  <text x="60" y="48" fill="#222222" font-family="{FONT_ESC}" font-size="24" font-weight="700">Agent \u5bf9\u8bdd\u65f6\u5e8f\uff08SSE\uff09</text>
  <text x="60" y="76" fill="#777777" font-family="{FONT_ESC}" font-size="16">POST /api/v1/agents/{{id}}/chat \u00b7 \u751f\u4ea7\u73af\u5883\u8bfb\u53d6 Published Version</text>
  <g transform="translate(60 110)">
    <rect x="0" y="0" width="1080" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="24" y="34" fill="#484848" font-family="{FONT_ESC}" font-size="14">1. \u7528\u6237\u8bf7\u6c42</text>
    <path d="M200 28h40" stroke="#c5c5c5" stroke-width="2"/><polygon points="240,28 232,24 232,32" fill="#c5c5c5"/>
    <text x="280" y="34" fill="#484848" font-family="{FONT_ESC}" font-size="14">2. \u9274\u6743\uff08JWT / API Key\uff09</text>
    <path d="M480 28h40" stroke="#c5c5c5" stroke-width="2"/><polygon points="520,28 512,24 512,32" fill="#c5c5c5"/>
    <text x="560" y="34" fill="#484848" font-family="{FONT_ESC}" font-size="14">3. \u52a0\u8f7d\u5df2\u53d1\u5e03\u914d\u7f6e</text>
    <path d="M760 28h40" stroke="#c5c5c5" stroke-width="2"/><polygon points="800,28 792,24 792,32" fill="#c5c5c5"/>
    <text x="840" y="34" fill="#484848" font-family="{FONT_ESC}" font-size="14">4. \u6784\u5efa ExecutionContext</text>
  </g>
  <g transform="translate(60 186)">
    <rect x="0" y="0" width="250" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="125" y="34" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">5. RAG \u6df7\u5408\u68c0\u7d22\uff08\u53ef\u9009\uff09</text>
    <path d="M258 28h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="290,28 282,24 282,32" fill="#c5c5c5"/>
    <rect x="298" y="0" width="220" height="56" rx="12" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
    <text x="408" y="34" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">6. Tool \u6267\u884c\uff08\u53ef\u9009\uff09</text>
    <path d="M526 28h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="558,28 550,24 550,32" fill="#c5c5c5"/>
    <rect x="566" y="0" width="220" height="56" rx="12" fill="#222222"/>
    <text x="676" y="34" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">7. LangChain4j \u2192 LLM</text>
    <path d="M794 28h32" stroke="#c5c5c5" stroke-width="2"/><polygon points="826,28 818,24 818,32" fill="#c5c5c5"/>
    <rect x="834" y="0" width="246" height="56" rx="12" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
    <text x="957" y="34" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="14">8. SSE delta / done</text>
  </g>
  <g transform="translate(60 262)">
    <rect x="0" y="0" width="1080" height="56" rx="12" fill="#ffffff" stroke="#e7e7e7"/>
    <text x="24" y="34" fill="#8b8b8b" font-family="{FONT_ESC}" font-size="13">\u5e76\u884c\u843d\u5e93\uff1aMessage \u00b7 Trace Span \u00b7 Token \u7528\u91cf \u00b7 \u6267\u884c\u72b6\u6001</text>
  </g>
</svg>
"""

for name, content in files.items():
    (OUT / name).write_text(content, encoding="utf-8")
    print("wrote", name)
