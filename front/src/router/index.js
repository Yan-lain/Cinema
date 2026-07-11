import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Movies from '../views/Movies.vue'
import Profile from '../views/Profile.vue'
import Vip from '../views/Vip.vue'
import Orders from '../views/Orders.vue'
import Settings from '../views/Settings.vue'
import Seat from '../views/Seat.vue'
import Payment from '../views/Payment.vue'
import Refund from '../views/Refund.vue'
import MovieDetail from '../views/MovieDetail.vue'
import AdminLogin from '../views/AdminLogin.vue'
import AdminHome from '../views/AdminHome.vue'
import Search from '../views/Search.vue'
import Cinemas from '../views/Cinemas.vue'
import CinemaDetail from '../views/CinemaDetail.vue'


const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/movies',
    name: 'Movies',
    component: Movies
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile
  },
  {
    path: '/vip',
    name: 'Vip',
    component: Vip
  },
  {
    path: '/orders',
    name: 'Orders',
    component: Orders
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings
  },
  {
    path: '/seat',
    name: 'Seat',
    component: Seat
  },
  {
    path: '/payment',
    name: 'Payment',
    component: Payment
  },
  {
    path: '/refund',
    name: 'Refund',
    component: Refund
  },
  {
    path: '/movie',
    name: 'MovieDetail',
    component: MovieDetail
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: AdminLogin
  },
  {
    path: '/admin',
    name: 'AdminHome',
    component: AdminHome
  },
  {
    path: '/search',
    name: 'Search',
    component: Search
  },
  {
    path: '/cinemas',
    name: 'Cinemas',
    component: Cinemas
  },
  {
    path: '/cinema-detail',
    name: 'CinemaDetail',
    component: CinemaDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
