<template>
  <div class="source-repo-links" :class="[`source-repo-links--${size}`, { 'source-repo-links--ghost': ghost }]">
    <a
      :href="BOX_SOURCE_REPOS.gitee"
      class="source-repo-links__btn"
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Gitee 仓库"
      title="Gitee"
    >
      <img
        class="source-repo-links__img source-repo-links__img--gitee"
        src="https://cdn.jsdelivr.net/npm/simple-icons@11.14.0/icons/gitee.svg"
        alt=""
      />
    </a>
    <a
      :href="BOX_SOURCE_REPOS.github"
      class="source-repo-links__btn"
      target="_blank"
      rel="noopener noreferrer"
      aria-label="GitHub 仓库"
      title="GitHub"
    >
      <img
        class="source-repo-links__img"
        src="https://cdn.jsdelivr.net/npm/simple-icons@11.14.0/icons/github.svg"
        alt=""
      />
    </a>
  </div>
</template>

<script setup lang="ts">
import { BOX_SOURCE_REPOS } from '../constants/sourceRepos'

withDefaults(
  defineProps<{
    size?: 'sm' | 'md'
    /** 浅色底上的描边按钮，适合演示信息条内嵌 */
    ghost?: boolean
  }>(),
  { size: 'md', ghost: true },
)
</script>

<style scoped>
.source-repo-links {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.source-repo-links__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--box-surface);
  border: 1px solid var(--box-border);
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    transform 0.15s ease;
}

.source-repo-links--ghost .source-repo-links__btn {
  background: color-mix(in srgb, var(--box-surface) 92%, transparent);
}

.source-repo-links__btn:hover {
  border-color: color-mix(in srgb, var(--box-ink) 22%, var(--box-border));
  transform: translateY(-1px);
}

.source-repo-links--md .source-repo-links__btn {
  width: 34px;
  height: 34px;
}

.source-repo-links--sm .source-repo-links__btn {
  width: 30px;
  height: 30px;
}

.source-repo-links--md .source-repo-links__img {
  width: 18px;
  height: 18px;
}

.source-repo-links--sm .source-repo-links__img {
  width: 16px;
  height: 16px;
}

.source-repo-links__img--gitee {
  filter: invert(18%) sepia(89%) saturate(4342%) hue-rotate(349deg) brightness(88%) contrast(93%);
}
</style>
