import request from './request'

export function getUserProfile() {
  return request({
    url: '/user/profile',
    method: 'GET',
  })
}

export function updateUserProfile(data) {
  return request({
    url: '/user/profile',
    method: 'PUT',
    data,
  })
}

export function updatePassword(data) {
  return request({
    url: '/user/password',
    method: 'PUT',
    data,
  })
}
