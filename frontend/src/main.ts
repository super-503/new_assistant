import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/image-viewer/style/css'
import './styles/design-system/index.css'
import './style.css'
import 'virtual:svg-icons-register'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/news-admin/review' },
    {
      path: '/news-admin',
      component: () => import('@/views/newsAdmin/NewsAdminPage.vue'),
      redirect: { name: 'NewsAdminReview' },
      children: [
        { path: 'review', name: 'NewsAdminReview', component: () => import('@/views/newsAdmin/review/ReviewPage.vue') },
        { path: 'library', name: 'NewsAdminLibrary', component: () => import('@/views/newsAdmin/library/LibraryPage.vue') },
        { path: 'channels', name: 'NewsAdminChannels', component: () => import('@/views/newsAdmin/channels/ChannelsPage.vue') },
        { path: 'papers', name: 'PaperSearch', component: () => import('@/views/paperSearch/PaperSearchPage.vue') },
        { path: 'settings', name: 'NewsAdminSettings', component: () => import('@/views/newsAdmin/settings/SettingsPage.vue') }
      ]
    }
  ]
})

document.documentElement.classList.add('dark')
createApp(App).use(createPinia()).use(router).mount('#app')
