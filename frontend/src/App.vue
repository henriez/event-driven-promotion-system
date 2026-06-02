<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

interface Promotion {
  id?: number
  title: string
  description?: string
  price: number
  originalPrice?: number
  category: string
  storeId: string
  url: string
  validUntil?: string
  upvotes?: number
}

const promotions = ref<Promotion[]>([])
const hotDeal = ref<Promotion | null>(null)
const showHotDeal = ref(false)
const loading = ref(true)
let eventSource: EventSource | null = null
const apiBase = import.meta.env.VITE_API_BASE_URL || ''

const selectedCategory = ref('')
const searchQuery = ref('')
const categories = ['', 'Electronics', 'Food', 'Fashion']

const snackbar = ref(false)
const snackbarText = ref('')
const snackbarColor = ref('success')

const token = ref('')
const userName = ref('')
const userEmail = ref('')
const isLoggedIn = computed(() => !!token.value)

const authDialog = ref(false)
const authTab = ref('login')

const loginForm = ref<any>(null)
const loginEmail = ref('')
const loginPassword = ref('')
const loginError = ref('')
const loggingIn = ref(false)

const registerForm = ref<any>(null)
const regName = ref('')
const regEmail = ref('')
const regPassword = ref('')
const regConfirm = ref('')
const regError = ref('')
const registering = ref(false)

const rules = {
  required: (v: string) => !!v || 'Required',
  email: (v: string) => /.+@.+\..+/.test(v) || 'Invalid email',
  match: (v: string) => v === regPassword.value || 'Passwords mismatch'
}

const liveCount = computed(() => promotions.value.length)
const skeletonItems = Array.from({ length: 8 }, (_, i) => i)

function discountPercent(original: number, current: number): number {
  return Math.round((1 - current / original) * 100)
}

function formatPrice(n: number): string {
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function resetAuthForms() {
  loginEmail.value = ''
  loginPassword.value = ''
  loginError.value = ''
  regName.value = ''
  regEmail.value = ''
  regPassword.value = ''
  regConfirm.value = ''
  regError.value = ''
  if (loginForm.value) loginForm.value.resetValidation()
  if (registerForm.value) registerForm.value.resetValidation()
}

async function doLogin() {
  const { valid } = await loginForm.value.validate()
  if (!valid) return

  loginError.value = ''
  loggingIn.value = true
  try {
    const res = await fetch(`${apiBase}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: loginEmail.value.trim(), password: loginPassword.value }),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || 'Login failed')
    }
    const data = await res.json()
    token.value = data.token
    userName.value = loginEmail.value.trim().split('@')[0] ?? ''
    userEmail.value = loginEmail.value.trim()
    authDialog.value = false
    snackbarColor.value = 'success'
    snackbarText.value = 'Welcome back!'
    snackbar.value = true
    resetAuthForms()
  } catch (e: any) {
    loginError.value = e.message || 'Login failed'
  } finally {
    loggingIn.value = false
  }
}

async function doRegister() {
  const { valid } = await registerForm.value.validate()
  if (!valid) return

  regError.value = ''
  registering.value = true
  try {
    const res = await fetch(`${apiBase}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: regName.value.trim(),
        email: regEmail.value.trim(),
        password: regPassword.value,
      }),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || 'Registration failed')
    }
    const data = await res.json()
    token.value = data.token
    userName.value = regName.value.trim()
    userEmail.value = regEmail.value.trim()
    authDialog.value = false
    snackbarColor.value = 'success'
    snackbarText.value = 'Account created successfully!'
    snackbar.value = true
    resetAuthForms()
  } catch (e: any) {
    regError.value = e.message || 'Registration failed'
  } finally {
    registering.value = false
  }
}

function openAuth(tab: string) {
  authTab.value = tab
  authDialog.value = true
}

function logout() {
  token.value = ''
  userName.value = ''
  userEmail.value = ''
  snackbarColor.value = 'info'
  snackbarText.value = 'You have been logged out.'
  snackbar.value = true
}

async function fetchHistory() {
  try {
    const params = new URLSearchParams()
    if (selectedCategory.value) params.set('category', selectedCategory.value)
    if (searchQuery.value) params.set('search', searchQuery.value)
    const qs = params.toString()
    const url = `${apiBase}/api/promotions${qs ? '?' + qs : ''}`
    const res = await fetch(url)
    promotions.value = await res.json()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function connectSSE() {
  eventSource = new EventSource(`${apiBase}/api/stream`)

  eventSource.onmessage = (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data) as Promotion
      if (!selectedCategory.value || data.category.toLowerCase() === selectedCategory.value.toLowerCase()) {
        promotions.value.push(data)
      }
    } catch (e) {
      console.error('Failed to parse SSE event', e)
    }
  }

  eventSource.addEventListener('hot-deal', (event: Event) => {
    try {
      const data = JSON.parse((event as MessageEvent).data) as Promotion
      hotDeal.value = data
      showHotDeal.value = true
    } catch (e) {
      console.error('Failed to parse hot-deal SSE event', e)
    }
  })

  eventSource.onerror = (e) => {
    console.warn('SSE connection error, will auto-reconnect', e)
  }
}

function getCategoryColor(category: string) {
  const cat = category.toLowerCase()
  if (cat === 'electronics') return 'info'
  if (cat === 'food') return 'success'
  if (cat === 'fashion') return 'pink'
  return 'primary'
}

function getCategoryHex(category: string) {
  const cat = category.toLowerCase()
  if (cat === 'electronics') return '#3b82f6'
  if (cat === 'food') return '#22c55e'
  if (cat === 'fashion') return '#ec4899'
  return '#FF6B00'
}

async function upvotePromotion(id: number | undefined) {
  if (id === undefined) return
  try {
    await fetch(`${apiBase}/api/promotions/${id}/upvote`, { method: 'POST' })
    const promo = promotions.value.find(p => p.id === id)
    if (promo) {
      promo.upvotes = (promo.upvotes || 0) + 1
    }
  } catch (e) {
    console.error('Upvote failed', e)
  }
}

onMounted(() => {
  fetchHistory()
  connectSSE()
})

onUnmounted(() => {
  eventSource?.close()
})

watch(selectedCategory, () => {
  loading.value = true
  fetchHistory()
})

let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loading.value = true
    fetchHistory()
  }, 300)
})
</script>

<template>
  <v-app>
    <v-app-bar elevation="0" class="px-2 px-sm-6 glass-bar">
      <v-icon icon="mdi-fire" color="primary" size="x-large" class="mr-3" />
      <v-app-bar-title class="font-weight-black text-h6">
        Deal<span class="text-primary">Aggregator</span>
      </v-app-bar-title>

      <v-spacer />

      <v-chip v-if="eventSource" color="success" size="small" variant="flat" class="mr-6 px-3 live-chip">
        <v-icon start icon="mdi-broadcast" size="small" />
        {{ liveCount }} Live
      </v-chip>

      <div v-if="isLoggedIn" class="d-flex align-center bg-background rounded-pill pl-2 pr-1 py-1">
        <span class="text-subtitle-2 font-weight-medium mr-3 ml-2">{{ userName }}</span>
        <v-btn icon="mdi-logout" variant="text" size="small" color="grey-lighten-1" @click="logout" />
      </div>

      <div v-else class="d-flex align-center ga-3">
        <v-btn variant="text" @click="openAuth('login')">Login</v-btn>
        <v-btn variant="flat" color="primary" @click="openAuth('register')">Register</v-btn>
      </div>
    </v-app-bar>

    <v-main class="bg-background">
      <div class="hero-glow">
        <v-container class="pt-16 pb-16" max-width="1400">
          <v-row justify="center" class="mb-12">
            <v-col cols="12" md="8" lg="6" class="text-center">
              <v-icon icon="mdi-flash" size="64" color="primary" class="mb-4" />
              <h1 class="text-h3 font-weight-black mb-4">Real-Time Promotions</h1>
              <p class="text-h6 font-weight-regular text-grey-lighten-1">
                Instant deals pushed directly to your feed. No refreshing required.
              </p>
            </v-col>
          </v-row>
        </v-container>
      </div>

      <v-container class="pb-16" max-width="1400">
        <v-row class="mb-6 align-center">
          <v-col cols="12" sm="6" md="4">
            <v-text-field
              v-model="searchQuery"
              label="Search promotions"
              prepend-inner-icon="mdi-magnify"
              variant="solo-filled"
              flat
              hide-details
              clearable
              density="compact"
              class="search-field"
            />
          </v-col>
          <v-col cols="12" sm="6" md="8" class="d-flex align-center ga-2">
            <span class="text-caption text-grey-lighten-1 font-weight-bold mr-1">Category:</span>
            <v-chip
              v-for="cat in categories"
              :key="cat"
              :variant="selectedCategory === cat ? 'flat' : 'outlined'"
              :color="cat ? getCategoryColor(cat) : 'grey'"
              size="small"
              class="font-weight-bold text-uppercase"
              @click="selectedCategory = selectedCategory === cat ? '' : cat"
            >
              {{ cat || 'All' }}
            </v-chip>
          </v-col>
        </v-row>

        <div v-if="loading">
          <v-row>
            <v-col v-for="i in skeletonItems" :key="i" cols="12" sm="6" md="4" xl="3">
              <div class="skeleton-card rounded-xl">
                <div class="skeleton-accent" />
                <div class="pa-6">
                  <div class="d-flex align-center mb-4">
                    <div class="skeleton-chip" />
                    <div class="skeleton-store ml-auto" />
                  </div>
                  <div class="skeleton-title mb-3" />
                  <div class="skeleton-line mb-2" />
                  <div class="skeleton-line w-75 mb-6" />
                  <div class="d-flex align-center ga-3 mb-6">
                    <div class="skeleton-price" />
                    <div class="skeleton-old-price" />
                    <div class="skeleton-badge ml-auto" />
                  </div>
                  <div class="skeleton-btn" />
                </div>
              </div>
            </v-col>
          </v-row>
        </div>

        <div v-else-if="promotions.length === 0">
          <v-row justify="center">
            <v-col cols="12" sm="8" md="6" class="text-center py-16">
              <v-card color="surface" variant="outlined" class="pa-12 border-dashed rounded-xl">
                <v-icon icon="mdi-inbox-outline" size="80" color="grey-darken-1" class="mb-6" />
                <h2 class="text-h5 font-weight-bold mb-2">No deals available yet</h2>
                <p class="text-body-1 text-grey">Waiting for the first live promotion to arrive over the stream.</p>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <transition-group v-else tag="div" name="stagger" class="v-row ga-4">
          <v-col
            v-for="(promo, index) in promotions"
            :key="promo.title + promo.storeId + index"
            cols="12"
            sm="6"
            md="4"
            xl="3"
            :style="{ animationDelay: index * 50 + 'ms' }"
          >
            <v-card class="promo-card d-flex flex-column h-100 rounded-xl">
              <div
                class="card-accent-line rounded-t-xl"
                :style="{ background: getCategoryHex(promo.category) }"
              />

              <v-card-item class="pt-4 pb-0 px-6">
                <template #prepend>
                  <v-chip size="small" :color="getCategoryColor(promo.category)" variant="tonal" class="text-uppercase font-weight-bold">
                    {{ promo.category }}
                  </v-chip>
                </template>
                <template #append>
                  <div class="text-caption text-grey-lighten-1 font-weight-medium">
                    <v-icon icon="mdi-store" size="x-small" class="mr-1" />
                    {{ promo.storeId }}
                  </div>
                </template>
              </v-card-item>

              <v-card-text class="flex-grow-1 px-6 py-4">
                <h3 class="text-h6 font-weight-bold mb-2 text-truncate" :title="promo.title">{{ promo.title }}</h3>
                <p class="text-body-2 text-grey-lighten-1 mb-6 text-desc">
                  {{ promo.description || 'No description provided.' }}
                </p>

                <div class="d-flex align-center flex-wrap ga-2">
                  <span class="text-h4 font-weight-black text-primary">${{ formatPrice(Number(promo.price)) }}</span>
                  <span v-if="promo.originalPrice && promo.originalPrice > promo.price" class="text-h6 text-decoration-line-through text-grey-darken-1">
                    ${{ formatPrice(Number(promo.originalPrice)) }}
                  </span>
                  <v-chip v-if="promo.originalPrice && promo.originalPrice > promo.price" color="success" size="small" class="font-weight-bold ml-auto">
                    {{ discountPercent(Number(promo.originalPrice), Number(promo.price)) }}% OFF
                  </v-chip>
                </div>
              </v-card-text>

              <v-card-actions class="px-6 pb-6 pt-0 mt-auto d-flex ga-3">
                <v-btn :href="promo.url" target="_blank" color="primary" variant="flat" size="large" class="flex-grow-1 rounded-lg">
                  <v-icon start icon="mdi-open-in-new" />
                  View Deal
                </v-btn>
                <v-btn
                  variant="tonal"
                  color="primary"
                  size="large"
                  class="rounded-lg px-4"
                  :disabled="promo.id === undefined"
                  @click="upvotePromotion(promo.id)"
                  title="Upvote"
                >
                  <v-icon :class="{ 'mr-2': promo.upvotes !== undefined }" icon="mdi-fire" />
                  <span v-if="promo.upvotes !== undefined" class="font-weight-bold">{{ promo.upvotes }}</span>
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
        </transition-group>
      </v-container>
    </v-main>

    <v-dialog v-model="authDialog" @after-leave="resetAuthForms" max-width="420">
      <v-card class="rounded-xl auth-dialog-card overflow-hidden">
        <v-tabs v-model="authTab" color="primary" bg-color="rgba(255,255,255,0.02)" grow slider-color="primary">
          <v-tab value="login" class="font-weight-bold text-subtitle-1 text-none">Login</v-tab>
          <v-tab value="register" class="font-weight-bold text-subtitle-1 text-none">Register</v-tab>
        </v-tabs>

        <v-window v-model="authTab">
          <v-window-item value="login">
            <v-card-text class="pa-6">
              <v-form ref="loginForm" @submit.prevent="doLogin">
                <v-text-field v-model="loginEmail" :rules="[rules.required, rules.email]" label="Email address" prepend-inner-icon="mdi-email-outline" class="mb-3" />
                <v-text-field v-model="loginPassword" :rules="[rules.required]" label="Password" type="password" prepend-inner-icon="mdi-lock-outline" class="mb-4" />

                <div class="error-holder">
                  <v-alert v-if="loginError" type="error" variant="tonal" density="compact" class="mb-4">{{ loginError }}</v-alert>
                </div>

                <v-btn type="submit" color="primary" size="x-large" block :loading="loggingIn" class="auth-btn">
                  Sign In
                </v-btn>
              </v-form>
            </v-card-text>
          </v-window-item>

          <v-window-item value="register">
            <v-card-text class="pa-6">
              <v-form ref="registerForm" @submit.prevent="doRegister">
                <v-text-field v-model="regName" :rules="[rules.required]" label="Full Name" prepend-inner-icon="mdi-account-outline" class="mb-3" />
                <v-text-field v-model="regEmail" :rules="[rules.required, rules.email]" label="Email address" prepend-inner-icon="mdi-email-outline" class="mb-3" />
                <v-text-field v-model="regPassword" :rules="[rules.required]" label="Password" type="password" prepend-inner-icon="mdi-lock-outline" class="mb-3" />
                <v-text-field v-model="regConfirm" :rules="[rules.required, rules.match]" label="Confirm Password" type="password" prepend-inner-icon="mdi-lock-check-outline" class="mb-4" />

                <div class="error-holder">
                  <v-alert v-if="regError" type="error" variant="tonal" density="compact" class="mb-4">{{ regError }}</v-alert>
                </div>

                <v-btn type="submit" color="primary" size="x-large" block :loading="registering" class="auth-btn">
                  Create Account
                </v-btn>
              </v-form>
            </v-card-text>
          </v-window-item>
        </v-window>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="showHotDeal" location="top" timeout="10000" class="hot-snackbar" transition="slide-y-reverse-transition">
      <div class="hot-deal-body">
        <div class="hot-deal-icon-box">
          <v-icon icon="mdi-fire" size="32" class="hot-glow-icon" />
        </div>
        <div class="hot-deal-text-box">
          <div class="text-h6 font-weight-black lh-sm">HOT DEAL</div>
          <div class="text-body-1 font-weight-medium mt-1">{{ hotDeal?.title }}</div>
          <div class="text-h5 font-weight-black text-primary mt-1">${{ formatPrice(Number(hotDeal?.price || 0)) }}</div>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" color="grey-lighten-1" @click="showHotDeal = false" class="hot-close-btn" />
      </div>
    </v-snackbar>

    <v-snackbar v-model="snackbar" :color="snackbarColor" location="bottom center" timeout="4000" rounded="pill" class="mb-4">
      <div class="d-flex align-center justify-center font-weight-medium text-body-1">
        {{ snackbarText }}
      </div>
    </v-snackbar>
  </v-app>
</template>

<style>
body {
  background: #070B14;
}

.glass-panel {
  background: rgba(26, 35, 50, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes glow-pulse {
  0%, 100% {
    filter: drop-shadow(0 0 6px rgba(255, 107, 0, 0.5));
  }
  50% {
    filter: drop-shadow(0 0 18px rgba(255, 107, 0, 0.9));
  }
}

.stagger-enter-active {
  animation: slideUp 0.4s ease-out both;
}

.stagger-leave-active {
  animation: slideUp 0.25s ease-in reverse both;
  position: absolute;
}

.stagger-move {
  transition: transform 0.4s ease;
}
</style>

<style scoped>
.glass-bar {
  background: rgba(7, 11, 20, 0.72) !important;
  backdrop-filter: blur(12px) saturate(1.5);
  -webkit-backdrop-filter: blur(12px) saturate(1.5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.live-chip {
  animation: pulse 2s ease-in-out infinite;
}

.hero-glow {
  position: relative;
}

.hero-glow::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 50% at 50% 0%, rgba(255, 107, 0, 0.12) 0%, transparent 70%),
    radial-gradient(ellipse 50% 40% at 75% 80%, rgba(0, 229, 255, 0.06) 0%, transparent 60%);
  pointer-events: none;
}

.border-dashed {
  border-style: dashed !important;
  border-width: 2px !important;
}

.text-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.promo-card {
  background: linear-gradient(145deg, #1a2332 0%, #1e293b 100%) !important;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.5, 1),
              border-color 0.3s ease,
              box-shadow 0.3s ease;
  position: relative;
  overflow: hidden;
}

.promo-card:hover {
  transform: translateY(-6px);
  border-color: #FF6B00;
  box-shadow: 0 12px 40px rgba(255, 107, 0, 0.12);
}

.card-accent-line {
  height: 4px;
  width: 100%;
  flex-shrink: 0;
}

.skeleton-card {
  background: linear-gradient(145deg, #1a2332 0%, #1e293b 100%);
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.skeleton-accent {
  height: 4px;
  background: rgba(255, 255, 255, 0.04);
}

.skeleton-chip {
  width: 72px;
  height: 24px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-store {
  width: 56px;
  height: 14px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-title {
  width: 85%;
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line {
  width: 100%;
  height: 12px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-price {
  width: 72px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-old-price {
  width: 52px;
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-badge {
  width: 56px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-btn {
  width: 100%;
  height: 42px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.auth-dialog-card {
  background: rgba(26, 35, 50, 0.85) !important;
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.error-holder {
  min-height: 48px;
}

.auth-btn {
  transition: filter 0.2s ease, transform 0.2s ease;
}

.auth-btn:hover:not(:disabled) {
  filter: brightness(1.15);
  transform: scale(1.02);
}

.auth-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.hot-snackbar :deep(.v-snackbar__wrapper) {
  background: #1a2332 !important;
  border: 2px solid #FF6B00 !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 40px rgba(255, 107, 0, 0.2) !important;
  padding: 0 !important;
}

.hot-deal-body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  min-width: 340px;
}

.hot-deal-icon-box {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: rgba(255, 107, 0, 0.12);
}

.hot-glow-icon {
  color: #FF6B00;
  animation: glow-pulse 1.5s ease-in-out infinite;
}

.hot-deal-text-box {
  flex: 1;
  min-width: 0;
}

.hot-close-btn {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: -4px;
}

.lh-sm {
  line-height: 1.2;
}

.v-tab.v-tab {
  text-transform: none;
  letter-spacing: 0;
}

.search-field :deep(.v-field__input) {
  font-size: 0.9rem;
}
</style>