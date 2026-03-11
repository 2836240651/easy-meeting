import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/ai-test',
      name: 'AITest',
      component: () => import('../views/AITest.vue')
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/meeting/:meetingId?',
      name: 'Meeting',
      component: () => import('../views/Meeting.vue'),
      meta: { requiresAuth: true }
    },
    // 屏幕共享悬浮窗口路由
    {
      path: '/screen-share-topbar',
      name: 'ScreenShareTopBar',
      component: () => import('../views/ScreenShareTopBar.vue')
    },
    {
      path: '/screen-share-video',
      name: 'ScreenShareVideo',
      component: () => import('../views/ScreenShareVideo.vue')
    },
    {
      path: '/screen-share-chat',
      name: 'ScreenShareChat',
      component: () => import('../views/ScreenShareChat.vue')
    },
    {
      path: '/border',
      name: 'ScreenShareBorder',
      component: () => import('../views/ScreenShareBorder.vue')
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 屏幕共享悬浮窗口不需要认证
  const overlayRoutes = ['ScreenShareTopBar', 'ScreenShareVideo', 'ScreenShareChat', 'ScreenShareBorder']
  if (overlayRoutes.includes(to.name)) {
    next()
    return
  }
  
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router