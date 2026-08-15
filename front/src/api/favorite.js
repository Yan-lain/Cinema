import request from '../utils/request'

export const favoriteApi = {
  add: async (userId, movieId) => {
    return await request.post('/favorite/add', { userId, movieId })
  },

  remove: async (userId, movieId) => {
    return await request.delete('/favorite/remove', { data: { userId, movieId } })
  },

  deleteById: async (id) => {
    return await request.delete(`/favorite/delete/${id}`)
  },

  list: async (userId, limit = 20) => {
    return await request.get('/favorite/list', { params: { userId, limit } })
  },

  count: async (userId) => {
    return await request.get('/favorite/count', { params: { userId } })
  },

  check: async (userId, movieId) => {
    return await request.get('/favorite/check', { params: { userId, movieId } })
  }
}
