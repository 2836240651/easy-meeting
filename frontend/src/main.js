import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import './styles/element-dark-theme.css'
import './styles/dark-theme.css'

createApp(App)
  .use(router)
  .use(ElementPlus)
  .mount('#app')