import { chromium } from 'playwright-core'
import { mkdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectDirectory = path.resolve(frontendDirectory, '..')
const configFile = path.join(projectDirectory, 'deploy', 'local.env.properties')
const edgeExecutable = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const HEARTBEAT_FLUSH_WAIT_MS = 5_500
const properties = Object.fromEntries((await readFile(configFile, 'utf8'))
  .split(/\r?\n/)
  .map(line => line.trim())
  .filter(line => line && !line.startsWith('#') && line.includes('='))
  .map(line => {
    const separator = line.indexOf('=')
    return [line.slice(0, separator), line.slice(separator + 1)]
  }))

const browser = await chromium.launch({ executablePath: edgeExecutable, headless: true })
const context = await browser.newContext({ baseURL: 'http://127.0.0.1:5173', locale: 'zh-CN' })
const page = await context.newPage()
const failures = []
const coloredLogs = []

page.on('pageerror', error => failures.push(`PAGE_ERROR ${error.message}`))
page.on('requestfailed', request => {
  const errorText = request.failure()?.errorText
  const isBestEffortTelemetryAbort = request.url().includes('/api/v1/dev/client-events')
    && errorText === 'net::ERR_ABORTED'
  if (!isBestEffortTelemetryAbort) {
    failures.push(`REQUEST_FAILED ${request.method()} ${request.url()} ${errorText}`)
  }
})
page.on('response', response => {
  if (response.status() >= 500) failures.push(`HTTP_${response.status()} ${response.url()}`)
})
page.on('console', message => {
  if (/\[(UI ACTION|HTTP ERROR|FRONTEND ERROR|BACKEND)/.test(message.text())) coloredLogs.push(message.text())
})

try {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(properties.APP_BOOTSTRAP_ADMIN_USERNAME)
  await page.getByPlaceholder('密码').fill(properties.APP_BOOTSTRAP_ADMIN_PASSWORD)
  await page.getByRole('button', { name: /进入资产工作台/ }).click()
  await page.waitForURL('**/assets')
  await page.getByRole('heading', { name: '资产工作台' }).waitFor()
  await page.getByText(/本地链路正常|降级运行/).waitFor({ timeout: 10_000 })

  const routes = [
    ['人员与居住', '/people'],
    ['租赁与合同', '/rentals'],
    ['物业收费', '/fees'],
    ['空间地图', '/map'],
    ['系统设置', '/settings']
  ]
  for (const [label, route] of routes) {
    await page.getByRole('button', { name: new RegExp(label) }).click()
    await page.waitForURL(`**${route}`)
    await page.getByRole('heading', { name: label, level: 2 }).waitFor()
  }

  await page.getByTitle('消息中心').click()
  await page.waitForURL('**/notifications')
  await page.getByRole('heading', { name: '消息中心', level: 2 }).waitFor()
  await page.getByRole('button', { name: '资产工作台', exact: true }).click()
  await page.waitForURL('**/assets')
  await page.getByText('房产总量').waitFor()
  // Allow one heartbeat interval to flush the queued semantic UI actions.
  await page.waitForTimeout(HEARTBEAT_FLUSH_WAIT_MS)

  const evidenceDirectory = path.join(projectDirectory, 'database', 'evidence', 'phase2a-stabilization')
  await mkdir(evidenceDirectory, { recursive: true })
  await page.screenshot({ path: path.join(evidenceDirectory, 'edge-assets-page.png'), fullPage: true })

  if (failures.length) throw new Error(failures.join('\n'))
  console.log(JSON.stringify({
    result: 'EDGE_E2E_PASS',
    browser: 'Microsoft Edge',
    routes: ['/assets', ...routes.map(([, route]) => route), '/notifications'],
    clientActionLogsObserved: coloredLogs.length
  }))
} finally {
  await browser.close()
}
