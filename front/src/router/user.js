import Home from '../views/user/Home.vue'
import Movies from '../views/user/Movies.vue'
import Profile from '../views/user/Profile.vue'
import Vip from '../views/user/Vip.vue'
import Orders from '../views/user/Orders.vue'
import Settings from '../views/user/Settings.vue'
import Seat from '../views/user/Seat.vue'
import Payment from '../views/user/Payment.vue'
import Refund from '../views/user/Refund.vue'
import MovieDetail from '../views/user/MovieDetail.vue'
import Search from '../views/user/Search.vue'
import Cinemas from '../views/user/Cinemas.vue'
import CinemaDetail from '../views/user/CinemaDetail.vue'

const userRoutes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    path: '/movies',
    name: 'Movies',
    component: Movies,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    // 个人中心
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: {
      requiresAuth: true,
      role: 'user'
    }
  },
  {
    // 会员中心
    path: '/vip',
    name: 'Vip',
    component: Vip,
    meta: {
      requiresAuth: true,
      role: 'user'
    }
  },
  {
    // 订单中心
    path: '/orders',
    name: 'Orders',
    component: Orders,
    meta: {
      requiresAuth: true,
      role: 'user'
    }
  },
  {
    // 设置中心
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    // 座位选择中心
    path: '/seat',
    name: 'Seat',
    component: Seat,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    // 支付中心
    path: '/payment',
    name: 'Payment',
    component: Payment,
    meta: {
      requiresAuth: true,
      role: 'user'
    }
  },
  {
    // 退款中心
    path: '/refund',
    name: 'Refund',
    component: Refund,
    meta: {
      requiresAuth: true,
      role: 'user'
    }
  },
  {
    // 电影详情中心
    path: '/movie',
    name: 'MovieDetail',
    component: MovieDetail,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    path: '/search',
    name: 'Search',
    component: Search,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    path: '/cinemas',
    name: 'Cinemas',
    component: Cinemas,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  },
  {
    path: '/cinema-detail',
    name: 'CinemaDetail',
    component: CinemaDetail,
    meta: {
      requiresAuth: false,
      role: 'user'
    }
  }
]

export default userRoutes