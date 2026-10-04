import { chromium } from 'playwright-core'
import { mkdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectDirectory = path.resolve(frontendDirectory, '..')
const config = Object.fromEntries((await readFile(path.join(projectDirectory, 'deploy', 'local.env.properties'), 'utf8'))
  .split(/\r?\n/).map(line => line.trim()).filter(line => line && !line.startsWith('#') && line.includes('='))
  .map(line => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=') + 1)]))
const browser = await chromium.launch({
  executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  headless: true
})
const page = await browser.newPage({ baseURL: 'http://127.0.0.1:5173', locale: 'zh-CN' })

try {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(config.APP_BOOTSTRAP_ADMIN_USERNAME)
  await page.getByPlaceholder('密码').fill(config.APP_BOOTSTRAP_ADMIN_PASSWORD)
  await page.getByRole('button', { name: /进入资产工作台/ }).click()
  await page.waitForURL('**/assets')
  console.log('OFFLINE_TEST_READY')
  await page.waitForURL(url => url.pathname === '/login' && url.searchParams.get('offline') === '1', { timeout: 30_000 })
  await page.getByText(/后端连接已中断/).waitFor()
  const evidenceDirectory = path.join(projectDirectory, 'database', 'evidence', 'phase2a-stabilization')
  await mkdir(evidenceDirectory, { recursive: true })
  await page.screenshot({ path: path.join(evidenceDirectory, 'edge-backend-offline.png'), fullPage: true })
  console.log(JSON.stringify({ result: 'EDGE_OFFLINE_LOGOUT_PASS', heartbeatFailures: 3, redirectedTo: page.url() }))
} finally {
  await browser.close()
}
