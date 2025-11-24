import request from '@/utils/request'

export function statUser(query) {
  return request({
    url: '/stat/user',
    method: 'get',
    params: query
  })
}

export function statOrder(query) {
  return request({
    url: '/stat/order',
    method: 'get',
    params: query
  })
}

export function statGoods(query) {
  return request({
    url: '/stat/goods',
    method: 'get',
    params: query
  })
}

export function statComment(query) {
  return request({
    url: '/stat/comment',
    method: 'get',
    params: query
  })
}

export function statCommentWordcloud(query) {
  return request({
    url: '/stat/comment/wordcloud',
    method: 'get',
    params: query
  })
}
