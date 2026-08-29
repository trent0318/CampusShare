import request from './request'

export function createReservation(data) {
  return request.post('/reservations', data)
}

export function listMyReservations(params) {
  return request.get('/reservations/mine', { params })
}

export function getReservation(id) {
  return request.get(`/reservations/${id}`)
}

export function cancelReservation(id, reason) {
  return request.delete(`/reservations/${id}`, {
    params: reason ? { reason } : {}
  })
}

export function checkinReservation(id) {
  return request.put(`/reservations/${id}/checkin`)
}

export function completeReservation(id) {
  return request.put(`/reservations/${id}/complete`)
}
