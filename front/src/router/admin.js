import AdminLogin from '@/views/admin/AdminLogin.vue'
import AdminHome from '@/views/admin/AdminHome.vue'

const adminRoutes = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: AdminLogin,
    meta: {
      requiresAuth: false,
      role: 'admin'
    }
  },
  {
    path: '/admin',
    name: 'AdminHome',
    component: AdminHome,
    meta: {
      requiresAuth: true,
      role: 'admin'
    }
  }
]

export default adminRoutes