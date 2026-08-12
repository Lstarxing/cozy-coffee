// 静态图片 CDN 配置：图片从阿里云 OSS 加载，不打包进小程序
// bucket: cozycoffee-srx · 访问域名见 application-local.yml storage.public-base-url
export const IMAGE_BASE_URL = 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/mobile'

export function imageUrl(name) {
  return `${IMAGE_BASE_URL}/${name}`
}
