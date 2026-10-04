import { chromium } from 'playwright-core'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectDirectory = path.resolve(frontendDirectory, '..')
const properties = Object.fromEntries((await readFile(path.join(projectDirectory, 'deploy', 'local.env.properties'), 'utf8'))
  .split(/\r?\n/)
  .map(line => line.trim())
  .filter(line => line && !line.startsWith('#') && line.includes('='))
  .map(line => {
    const separator = line.indexOf('=')
    return [line.slice(0, separator), line.slice(separator + 1)]
  }))

const browser = await chromium.launch({
  executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  headless: true
})
const page = await browser.newPage({ baseURL: 'http://127.0.0.1:5173', locale: 'zh-CN' })

try {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(properties.APP_BOOTSTRAP_ADMIN_USERNAME)
  await page.getByPlaceholder('密码').fill(properties.APP_BOOTSTRAP_ADMIN_PASSWORD)
  await page.getByRole('button', { name: /进入资产工作台/ }).click()
  await page.waitForURL('**/assets')
  console.log('EDGE_SESSION_READY_FOR_BACKEND_STOP')
  // Three failed five-second heartbeats are required before forced logout.
  // Leave startup/teardown scheduling headroom for Windows CI and local IDEs.
  await page.waitForURL(/\/login(?:\?|$)/, { timeout: 60_000 })
  await page.getByRole('button', { name: /进入资产工作台/ }).waitFor()
  console.log(JSON.stringify({ result: 'BACKEND_STOP_AUTO_LOGOUT_PASS', browser: 'Microsoft Edge' }))
} finally {
  await browser.close()
}
