import API_BASE_URL from '../api'

export const favoriteApi = {
  add: async (userId, movieId) => {
    const response = await fetch(`${API_BASE_URL}/favorite/add`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, movieId })
    })
    return response.json()
  },

  remove: async (userId, movieId) => {
    const response = await fetch(`${API_BASE_URL}/favorite/remove`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, movieId })
    })
    return response.json()
  },

  deleteById: async (id) => {
    const response = await fetch(`${API_BASE_URL}/favorite/delete/${id}`, {
      method: 'DELETE'
    })
    return response.json()
  },

  list: async (userId, limit = 20) => {
    const response = await fetch(`${API_BASE_URL}/favorite/list?userId=${userId}&limit=${limit}`)
    return response.json()
  },

  count: async (userId) => {
    const response = await fetch(`${API_BASE_URL}/favorite/count?userId=${userId}`)
    return response.json()
  },

  check: async (userId, movieId) => {
    const response = await fetch(`${API_BASE_URL}/favorite/check?userId=${userId}&movieId=${movieId}`)
    return response.json()
  }
}