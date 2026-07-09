import dayjs from 'dayjs'

export function formatDateTime(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

export function formatDateTimeFull(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDate(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

export function formatTime(date) {
  if (!date) return '-'
  return dayjs(date).format('HH:mm')
}

export function fromNow(date) {
  if (!date) return '-'
  return dayjs(date).fromNow()
}
