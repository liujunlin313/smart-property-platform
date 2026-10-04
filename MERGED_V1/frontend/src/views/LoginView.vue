<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { homeFor, isAllowedRoute } from '../router/index.js'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, OfficeBuilding, User } from '@element-plus/icons-vue'
import { apiMessage } from '../api/http.js'
import { useAuth } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const backendOffline = computed(() => route.query.offline === '1' || !auth.state.backendOnline)
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('身份验证成功')
    const requested = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? router.resolve(route.query.redirect)
      : null
    await router.replace(requested && isAllowedRoute(auth.state.account, requested)
      ? requested.fullPath
      : homeFor(auth.state.account))
  } catch (error) {
    ElMessage.error(apiMessage(error, '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="brand-story">
      <div class="brand-mark"><OfficeBuilding /></div>
      <div class="brand-copy">
        <p class="eyebrow light">MULTI-COMMUNITY PLATFORM</p>
        <h1>让房产层级、现实位置与业务关系，在一套系统里保持一致。</h1>
        <p class="lead">从小区、楼栋、单元到整套房产，采用可演进的数据库模型与清晰的权限边界。</p>
      </div>
      <div class="architecture-card">
        <div><span>01</span><b>资产骨架</b><small>小区 · 楼栋 · 单元 · 房产</small></div>
        <div><span>02</span><b>一致性</b><small>MyBatis · 事务 · 乐观锁</small></div>
        <div><span>03</span><b>演进路线</b><small>租赁 · 收费 · 地图 · 空间查询</small></div>
      </div>
      <div class="grid-decoration" />
    </section>

    <section class="login-zone">
      <div class="login-card">
        <div class="login-heading">
          <p class="eyebrow">SECURE ACCESS</p>
          <h2>欢迎回到栖云</h2>
          <p>使用系统账号进入多小区资产工作台</p>
        </div>

        <el-alert v-if="backendOffline" class="offline-alert" title="后端连接已中断，系统将在服务恢复后重新验证登录状态。" type="warning" :closable="false" show-icon />
        <el-form ref="formRef" :model="form" :rules="rules" size="large" data-action="登录系统" @submit.prevent="submit" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" autocomplete="username">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password autocomplete="current-password">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="login-button" data-action="登录系统" :loading="loading" @click="submit">
            进入资产工作台
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="login-note">
          <span class="secure-dot" />
          <p>JWT短期令牌 · Refresh Token轮换 · 后端权限校验</p>
        </div>
      </div>
      <p class="version">Phase 2A · Database V3 · 课程设计演示版</p>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(650px, 1.2fr) minmax(470px, .8fr); background: #f3f6f8; }
.brand-story { position: relative; overflow: hidden; padding: 64px 7vw; color: #f7fbfb; background: linear-gradient(145deg, #0f3740 0%, #0d4f50 54%, #167d72 100%); display: flex; flex-direction: column; justify-content: center; }
.brand-story::before { content: ""; position: absolute; width: 540px; height: 540px; border: 1px solid rgba(255,255,255,.12); border-radius: 50%; right: -140px; top: -180px; box-shadow: 0 0 0 80px rgba(255,255,255,.025), 0 0 0 160px rgba(255,255,255,.018); }
.brand-mark { position: absolute; top: 52px; left: 7vw; width: 48px; height: 48px; display: grid; place-items: center; color: #164e50; background: #edf9f6; border-radius: 15px; font-size: 24px; box-shadow: 0 12px 28px rgba(0,0,0,.16); }
.brand-copy { position: relative; z-index: 1; max-width: 760px; margin-top: 36px; }
.eyebrow.light { color: #93ddd2; }
h1 { max-width: 780px; margin: 0; font-family: Georgia, "Noto Serif SC", serif; font-weight: 500; font-size: clamp(44px, 4.2vw, 68px); line-height: 1.15; letter-spacing: -.035em; }
.lead { max-width: 630px; margin: 28px 0 0; color: #c7dfdd; font-size: 18px; line-height: 1.8; }
.architecture-card { position: relative; z-index: 1; display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; max-width: 820px; margin-top: 58px; overflow: hidden; background: rgba(255,255,255,.16); border: 1px solid rgba(255,255,255,.14); border-radius: 18px; backdrop-filter: blur(12px); }
.architecture-card > div { min-height: 130px; padding: 24px; background: rgba(9,45,50,.5); display: flex; flex-direction: column; }
.architecture-card span { color: #74cbbb; font: 700 12px/1 Consolas, monospace; }
.architecture-card b { margin-top: 18px; font-size: 16px; }
.architecture-card small { margin-top: 8px; color: #b7d3d0; line-height: 1.5; }
.grid-decoration { position: absolute; inset: 0; opacity: .04; background-image: linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px); background-size: 48px 48px; pointer-events: none; }
.login-zone { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px; }
.login-card { width: 100%; max-width: 440px; padding: 46px; background: #fff; border: 1px solid #dfe8eb; border-radius: 24px; box-shadow: 0 28px 70px rgba(31,63,74,.12); }
.login-heading h2 { margin: 0; color: #102a43; font: 500 36px/1.2 Georgia, "Noto Serif SC", serif; }
.login-heading > p:last-child { margin: 12px 0 32px; color: #718596; }
.login-button { width: 100%; height: 48px; margin-top: 8px; }
.offline-alert { margin: 0 0 20px; }
.login-note { display: flex; align-items: center; gap: 10px; margin-top: 28px; padding-top: 22px; border-top: 1px solid #edf1f3; color: #7c8f9c; font-size: 12px; }
.login-note p { margin: 0; }
.secure-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #20a57b; box-shadow: 0 0 0 5px #e6f6f0; }
.version { margin: 24px 0 0; color: #8b9aa4; font-size: 12px; letter-spacing: .04em; }
:deep(.el-input__wrapper) { min-height: 48px; border-radius: 10px; box-shadow: 0 0 0 1px #dce6eb inset; }
@media (max-width: 1280px) { .brand-story { padding-left: 5vw; padding-right: 5vw; } .brand-mark { left: 5vw; } .login-zone { padding: 40px; } }
</style>
