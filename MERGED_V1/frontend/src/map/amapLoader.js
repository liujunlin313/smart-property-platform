let loadPromise = null

export function amapJsConfigured() {
  return Boolean(import.meta.env.VITE_AMAP_JS_KEY && import.meta.env.VITE_AMAP_JS_SECURITY_CODE)
}

/** Load the official AMap JS API loader once. Web Service credentials never enter this module. */
export function loadAmap() {
  if (loadPromise) return loadPromise
  if (!amapJsConfigured()) return Promise.reject(new Error('缺少高德 Web 端（JS API）本地配置'))
  window._AMapSecurityConfig = { securityJsCode: import.meta.env.VITE_AMAP_JS_SECURITY_CODE }
  loadPromise = new Promise((resolve, reject) => {
    if (window.AMapLoader) return resolve(window.AMapLoader)
    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/loader.js'
    script.async = true
    script.onload = () => window.AMapLoader ? resolve(window.AMapLoader) : reject(new Error('高德 Loader 未就绪'))
    script.onerror = () => reject(new Error('高德 JS API Loader 加载失败'))
    document.head.appendChild(script)
  }).then(loader => loader.load({
    key: import.meta.env.VITE_AMAP_JS_KEY,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.MouseTool', 'AMap.PolyEditor']
  }))
  return loadPromise
}
