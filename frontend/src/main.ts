import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import App from './App.vue'

const vuetify = createVuetify({
  components,
  directives,
  icons: { defaultSet: 'mdi' },
  theme: {
    defaultTheme: 'dark',
    themes: {
      dark: {
        dark: true,
        colors: {
          background: '#070B14',
          surface: '#1a2332',
          primary: '#FF6B00',
          secondary: '#00E5FF',
          accent: '#FF8A00',
          error: '#ef4444',
          info: '#3b82f6',
          success: '#22c55e',
          warning: '#FF8A00',
        },
      },
    },
  },
  defaults: {
    VCard: {
      rounded: 'xl',
      elevation: 0,
    },
    VBtn: {
      rounded: 'pill',
      style: 'text-transform: none; font-weight: 700;',
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VDialog: {
      maxWidth: 450,
    },
  },
})

createApp(App).use(vuetify).mount('#app')