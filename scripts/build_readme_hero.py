# -*- coding: utf-8 -*-
"""Generate assets/readme/hero.svg (no embedded logo; README uses logo-readme.png separately)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
readme = ROOT / "assets" / "readme"
FONT_ESC = "system-ui, &quot;PingFang SC&quot;, &quot;Microsoft YaHei&quot;, sans-serif"

hero = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="400" viewBox="0 0 1200 400" role="img" aria-labelledby="heroTitle heroDesc">
  <title id="heroTitle">Box - \u4f01\u4e1a\u7ea7 AI Agent \u4e0e\u5de5\u4f5c\u6d41\u5e73\u53f0</title>
  <desc id="heroDesc">\u6784\u5efa\u3001\u7f16\u6392\u3001\u53d1\u5e03 AI \u667a\u80fd\u4f53\uff0c\u652f\u6301 RAG\u3001\u5de5\u5177\u4e0e\u5de5\u4f5c\u6d41\u3002</desc>
  <defs>
    <linearGradient id="boxGlow" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffffff"/>
      <stop offset="100%" stop-color="#f3f3f3"/>
    </linearGradient>
    <pattern id="grid" width="24" height="24" patternUnits="userSpaceOnUse">
      <path d="M 24 0 L 0 0 0 24" fill="none" stroke="#ececec" stroke-width="1"/>
    </pattern>
  </defs>

  <rect width="1200" height="400" rx="20" fill="#fafafa"/>
  <rect width="1200" height="400" rx="20" fill="url(#grid)" opacity="0.45"/>
  <rect x="28" y="28" width="1144" height="344" rx="16" fill="none" stroke="#e7e7e7" stroke-width="1.5"/>

  <text x="72" y="88" fill="#8b8b8b" font-family="{FONT_ESC}" font-size="16" letter-spacing="0.1em">ENTERPRISE AI AGENT PLATFORM</text>
  <text x="72" y="128" fill="#222222" font-family="{FONT_ESC}" font-size="52" font-weight="700">Box</text>
  <text x="72" y="158" fill="#5e5e5e" font-family="{FONT_ESC}" font-size="18">AI Agent &amp; Workflow Orchestration</text>

  <text x="72" y="218" fill="#484848" font-family="{FONT_ESC}" font-size="26" font-weight="600">\u4f01\u4e1a\u7ea7 AI Agent \u521b\u5efa\u3001\u7f16\u6392\u3001\u8fd0\u884c\u4e0e\u53d1\u5e03\u5e73\u53f0</text>
  <text x="72" y="252" fill="#777777" font-family="{FONT_ESC}" font-size="18">\u7edf\u4e00\u627f\u8f7d\u6a21\u578b\u3001\u77e5\u8bc6\u5e93\u3001\u5de5\u5177\u94fe\u4e0e\u5de5\u4f5c\u6d41\uff0c\u652f\u6301\u4ece\u539f\u578b\u9a8c\u8bc1\u5230\u751f\u4ea7\u53d1\u5e03\u7684\u5168\u751f\u547d\u5468\u671f\u3002</text>

  <g transform="translate(72 288)">
    <rect x="0" y="0" width="118" height="32" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="59" y="21" fill="#5e5e5e" text-anchor="middle" font-family="Consolas, Monaco, monospace" font-size="13">Java 21</text>
    <rect x="128" y="0" width="150" height="32" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="203" y="21" fill="#5e5e5e" text-anchor="middle" font-family="Consolas, Monaco, monospace" font-size="13">Spring Boot 3</text>
    <rect x="288" y="0" width="96" height="32" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="336" y="21" fill="#5e5e5e" text-anchor="middle" font-family="Consolas, Monaco, monospace" font-size="13">Vue 3</text>
    <rect x="394" y="0" width="140" height="32" rx="16" fill="#ffffff" stroke="#dcdcdc"/>
    <text x="464" y="21" fill="#5e5e5e" text-anchor="middle" font-family="Consolas, Monaco, monospace" font-size="13">LangChain4j</text>
    <rect x="544" y="0" width="168" height="32" rx="16" fill="#222222"/>
    <text x="628" y="21" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">Modular Monolith</text>
  </g>

  <g transform="translate(620 56)">
    <rect x="0" y="0" width="508" height="264" rx="18" fill="url(#boxGlow)" stroke="#e7e7e7" stroke-width="1.5"/>
    <text x="24" y="32" fill="#8b8b8b" font-family="Consolas, Monaco, monospace" font-size="13">AGENT RUNTIME</text>
    <g transform="translate(24 52)">
      <rect x="0" y="0" width="100" height="56" rx="10" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
      <text x="50" y="26" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u9274\u6743</text>
      <text x="50" y="44" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">JWT / API Key</text>
      <path d="M104 28h20" stroke="#c5c5c5" stroke-width="2"/><polygon points="124,28 116,24 116,32" fill="#c5c5c5"/>
      <rect x="128" y="0" width="100" height="56" rx="10" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
      <text x="178" y="26" fill="#222222" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">\u4e0a\u4e0b\u6587</text>
      <text x="178" y="44" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u5df2\u53d1\u5e03\u7248\u672c</text>
      <path d="M232 28h20" stroke="#c5c5c5" stroke-width="2"/><polygon points="252,28 244,24 244,32" fill="#c5c5c5"/>
      <rect x="256" y="0" width="88" height="56" rx="10" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
      <text x="300" y="26" fill="#222222" text-anchor="middle" font-family="system-ui, sans-serif" font-size="14" font-weight="600">RAG</text>
      <text x="300" y="44" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u6df7\u5408\u68c0\u7d22</text>
      <path d="M348 28h20" stroke="#c5c5c5" stroke-width="2"/><polygon points="368,28 360,24 360,32" fill="#c5c5c5"/>
      <rect x="372" y="0" width="88" height="56" rx="10" fill="#ffffff" stroke="#222222" stroke-width="1.5"/>
      <text x="416" y="26" fill="#222222" text-anchor="middle" font-family="system-ui, sans-serif" font-size="14" font-weight="600">Tool</text>
      <text x="416" y="44" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">MCP</text>
      <path d="M178 72h60" stroke="#c5c5c5" stroke-width="2"/>
      <path d="M208 72v12" stroke="#c5c5c5" stroke-width="2"/>
      <rect x="136" y="88" width="152" height="56" rx="10" fill="#222222"/>
      <text x="212" y="114" fill="#ffffff" text-anchor="middle" font-family="{FONT_ESC}" font-size="14" font-weight="600">LangChain4j</text>
      <text x="212" y="132" fill="#cbcbcb" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">\u6a21\u578b\u8c03\u7528\u4e0e\u5de5\u5177\u7f16\u6392</text>
      <path d="M292 116h24" stroke="#c5c5c5" stroke-width="2"/><polygon points="316,116 308,112 308,120" fill="#c5c5c5"/>
      <rect x="320" y="88" width="120" height="56" rx="10" fill="#ffffff" stroke="#48c79c" stroke-width="2"/>
      <text x="380" y="114" fill="#222222" text-anchor="middle" font-family="system-ui, sans-serif" font-size="14" font-weight="600">LLM</text>
      <text x="380" y="132" fill="#8b8b8b" text-anchor="middle" font-family="{FONT_ESC}" font-size="11">SSE \u6d41\u5f0f\u8f93\u51fa</text>
      <path d="M120 144v20 M212 144v20 M380 144v20" stroke="#c5c5c5" stroke-width="2"/>
      <rect x="64" y="168" width="112" height="48" rx="10" fill="#ffffff" stroke="#dcdcdc"/>
      <text x="120" y="196" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u6d88\u606f\u843d\u5e93</text>
      <rect x="192" y="168" width="112" height="48" rx="10" fill="#ffffff" stroke="#dcdcdc"/>
      <text x="248" y="196" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u94fe\u8def\u8ffd\u8e2a</text>
      <rect x="320" y="168" width="112" height="48" rx="10" fill="#ffffff" stroke="#dcdcdc"/>
      <text x="376" y="196" fill="#484848" text-anchor="middle" font-family="{FONT_ESC}" font-size="13">\u7528\u91cf\u7edf\u8ba1</text>
    </g>
  </g>
</svg>
"""

(readme / "hero.svg").write_text(hero, encoding="utf-8")
print(f"Wrote {(readme / 'hero.svg').stat().st_size} bytes")
