# Frontend

Vue 3 + JavaScript + Vite + Element Plus 的 Phase 2A 资产工作台。

开发模式：

```powershell
npm install
npm run dev
```

Vite 会把 `/api` 和 `/actuator` 转发到本机 Spring Boot。生产构建：

```powershell
npm run build
```

访问令牌只保存在内存中；页面刷新时通过 HttpOnly Cookie 调用刷新接口恢复登录，不把令牌写入 `localStorage`。管理按钮根据角色隐藏，但最终权限仍由后端强制执行。
