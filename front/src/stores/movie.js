import { defineStore } from 'pinia'
import API_BASE_URL from '../api'

export const useMovieStore = defineStore('movie', {
  state: () => ({
    movies: [],
    currentMovie: null,
    loading: false,
    error: null
  }),

  getters: {
    getMovies: (state) => state.movies,
    getCurrentMovie: (state) => state.currentMovie,
    getMovieById: (state) => (id) => state.movies.find(m => m.id === id),
    getStatusDistribution: (state) => {
      const dist = { showing: 0, upcoming: 0, classic: 0 }
      state.movies.forEach(m => {
        if (m.status && dist.hasOwnProperty(m.status)) {
          dist[m.status]++
        }
      })
      return dist
    }
  },

  actions: {
    async fetchMovies() {
      this.loading = true
      this.error = null
      try {
        const response = await fetch(`${API_BASE_URL}/movies`)
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        const data = await response.json()
        if (data.success) {
          this.movies = data.data
        } else {
          this.error = data.message || '获取电影数据失败'
        }
      } catch (error) {
        console.error('🔴 Fetch movies error:', error)
        this.error = '网络连接异常，请检查后端服务是否正常运行'
      } finally {
        this.loading = false
      }
    },

    async fetchMovieById(id) {
      this.loading = true
      this.error = null
      try {
        //console.log('📡 正在获取电影详情，id:', id)
        const response = await fetch(`${API_BASE_URL}/movies/${id}`)
        const data = await response.json()
       // console.log('📦 电影详情:', data)
        if (data.success) {
          this.currentMovie = data.data
         // console.log('✅ 电影详情加载成功:', data.data.title)
        } else {
          this.error = data.message
          console.log('❌ 电影详情加载失败:', data.message)
        }
      } catch (error) {
        console.error('🔴 Fetch movie error:', error)
        this.error = '网络错误'
      } finally {
        this.loading = false
      }
    },

    async searchMovies(keyword) {
      this.loading = true
      this.error = null
      try {
        //console.log('📡 正在搜索电影，关键词:', keyword)
        const response = await fetch(`${API_BASE_URL}/movies/search?keyword=${encodeURIComponent(keyword)}`)
        const data = await response.json()
        //console.log('📦 搜索结果:', data)
        if (data.success) {
          this.movies = data.data
         // console.log('✅ 搜索完成，找到', data.data.length, '部电影')
        } else {
          this.error = data.message
          console.log('❌ 搜索失败:', data.message)
        }
      } catch (error) {
        console.error('🔴 Search movies error:', error)
        this.error = '网络错误'
      } finally {
        this.loading = false
      }
    }
  }
})
