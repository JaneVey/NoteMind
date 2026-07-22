import request from './request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data,
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data,
  })
}
