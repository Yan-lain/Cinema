import { defineStore } from 'pinia'
import request from '../utils/request'

export const useMovieStore = defineStore('movie', {
  state: () => ({
    movies: [],
    total: 0,
    currentPage: 0,
    pageSize: 10,
    currentMovie: null,
    loading: false,
    error: null,
    searchKeyword: '',
    filterStatus: ''
  }),

  getters: {
    getMovies: (state) => state.movies,
    getCurrentMovie: (state) => state.currentMovie,
    getMovieById: (state) => (id) => state.movies.find(m => m.id === id),
    getTotalPages: (state) => Math.ceil(state.total / state.pageSize),
    hasNextPage: (state) => state.currentPage < Math.ceil(state.total / state.pageSize) - 1,
    hasPrevPage: (state) => state.currentPage > 0,
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
    async fetchMoviesPage(page = 0, size = 10, status = '') {
      this.loading = true
      this.error = null
      try {
        const params = { page, size }
        if (status) {
          params.status = status
        }
        const pageData = await request.get('/movies/page', { params })
        this.movies = pageData.data || []
        this.total = pageData.total || 0
        this.currentPage = pageData.page || page
        this.pageSize = pageData.size || size
        this.filterStatus = status
      } catch (error) {
        console.error('Fetch movies page error:', error)
        this.error = error.message || '获取电影数据失败'
      } finally {
        this.loading = false
      }
    },

    async fetchMovieDetail(id) {
      this.loading = true
      this.error = null
      try {
        this.currentMovie = await request.get(`/movies/${id}`)
      } catch (error) {
        console.error('Fetch movie detail error:', error)
        this.error = error.message || '网络错误'
      } finally {
        this.loading = false
      }
    },

    async searchMoviesPage(keyword, page = 0, size = 10) {
      this.loading = true
      this.error = null
      this.searchKeyword = keyword
      
      try {
        const pageData = await request.get('/movies/search/page', {
          params: { keyword, page, size }
        })
        this.movies = pageData.data || []
        this.total = pageData.total || 0
        this.currentPage = pageData.page || page
        this.pageSize = pageData.size || size
      } catch (error) {
        console.error('Search movies page error:', error)
        this.error = error.message || '网络错误'
      } finally {
        this.loading = false
      }
    },

    async loadMore() {
      if (!this.hasNextPage || this.loading) return
      
      try {
        const nextPage = this.currentPage + 1
        let pageData
        
        if (this.searchKeyword) {
          pageData = await request.get('/movies/search/page', {
            params: { keyword: this.searchKeyword, page: nextPage, size: this.pageSize }
          })
        } else {
          const params = { page: nextPage, size: this.pageSize }
          if (this.filterStatus) {
            params.status = this.filterStatus
          }
          pageData = await request.get('/movies/page', { params })
        }
        
        this.movies = [...this.movies, ...(pageData.data || [])]
        this.total = pageData.total || this.total
        this.currentPage = pageData.page || nextPage
      } catch (error) {
        console.error('Load more error:', error)
      }
    },

    async refresh() {
      if (this.searchKeyword) {
        await this.searchMoviesPage(this.searchKeyword, 0, this.pageSize)
      } else {
        await this.fetchMoviesPage(0, this.pageSize, this.filterStatus)
      }
    },

    clearSearch() {
      this.searchKeyword = ''
      this.filterStatus = ''
      this.movies = []
      this.total = 0
      this.currentPage = 0
    }
  }
})
