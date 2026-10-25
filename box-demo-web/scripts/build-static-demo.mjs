import { cpSync, mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execSync } from 'node:child_process'

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..')
const out = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist')

rmSync(out, { recursive: true, force: true })
mkdirSync(out, { recursive: true })

execSync('npm run build:demo', { cwd: join(root, 'box-web'), stdio: 'inherit', env: { ...process.env } })
execSync('npm run build:demo', {
  cwd: join(root, 'box-admin-web'),
  stdio: 'inherit',
  env: { ...process.env, VITE_BASE: '/admin/' },
})

cpSync(join(root, 'box-web', 'dist'), out, { recursive: true })
cpSync(join(root, 'box-admin-web', 'dist'), join(out, 'admin'), { recursive: true })

// Cloudflare Pages：双 SPA 回退（Workers 静态资源不要用此文件）
writeFileSync(
  join(out, '_redirects'),
  [
    '/admin/login    /admin/index.html  200',
    '/admin/*        /admin/index.html  200',
    '/*              /index.html         200',
  ].join('\n') + '\n',
  'utf8',
)

console.log('Static demo built at', out)
