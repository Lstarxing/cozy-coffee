// 图片加载统一配置
// IMAGE_BASE 是唯一图片基址：本地托管时指向网关（图片由网关 classpath 静态 /images 服务）；
// 上线配好 OSS 后改为 OSS 域名即可，其余代码零改动。
export const IMAGE_BASE =
  (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_IMAGE_BASE_URL) ||
  'http://127.0.0.1:9000/cozycoffee'
// 上线/真机预览：经 VITE_IMAGE_BASE_URL 覆盖为服务器 MinIO 反代，如 http://<公网IP>/media/cozycoffee

/** 静态营销图（about/index/origins） */
export function imageUrl(name) {
  return `${IMAGE_BASE}/images/mobile/${name}`
}

/** 商品/兑换/订单图片：http 全路径或 /static 本地打包资源原样返回；其余相对路径拼 IMAGE_BASE */
export function resolveImageUrl(url) {
  if (!url) return ''
  if (typeof url === 'string' && (url.startsWith('http') || url.startsWith('/static/'))) return url
  return `${IMAGE_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
