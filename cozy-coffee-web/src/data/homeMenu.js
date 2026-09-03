const MENU_IMAGE_BASE = import.meta.env.VITE_MENU_IMAGE_BASE || '/images/home/menu'
const PRODUCT_IMAGE_BASE = import.meta.env.VITE_IMAGE_BASE_URL || ''
const LOCAL_FALLBACK = '/images/beans.jpg'

function flavorImage(flavor, alt = '') {
  const base = MENU_IMAGE_BASE
  const name = `menu-featured-${flavor}`
  const sizes = [640, 960, 1200]
  return Object.freeze({
    avif: sizes.map(w => `${base}/${name}-${w}.avif ${w}w`).join(', '),
    webp: sizes.map(w => `${base}/${name}-${w}.webp ${w}w`).join(', '),
    jpg: sizes.map(w => `${base}/${name}-${w}.jpg ${w}w`).join(', '),
    fallback: `${base}/${name}-960.jpg`,
    localFallback: LOCAL_FALLBACK,
    alt
  })
}

function heroImage(alt = '') {
  const base = MENU_IMAGE_BASE
  const name = 'menu-hero-still'
  const sizes = [768, 1440, 1920]
  return Object.freeze({
    avif: sizes.map(w => `${base}/${name}-${w}.avif ${w}w`).join(', '),
    webp: sizes.map(w => `${base}/${name}-${w}.webp ${w}w`).join(', '),
    jpg: sizes.map(w => `${base}/${name}-${w}.jpg ${w}w`).join(', '),
    fallback: `${base}/${name}-1440.jpg`,
    localFallback: LOCAL_FALLBACK,
    alt
  })
}

/** 代表性分类图：复用 V2 商品图（网关 /images/v2，DB 商品图），VITE_IMAGE_BASE_URL 拼接 */
function productSeriesImage(path, alt = '') {
  return Object.freeze({
    fallback: `${PRODUCT_IMAGE_BASE}${path}`,
    localFallback: LOCAL_FALLBACK,
    alt
  })
}

/** Hero still life — fixed, does not change with flavor tags. 16:10 container. */
export const MENU_HERO_IMAGE = heroImage('手冲器具与杯具静物')

const FLAVOR_IMAGES = Object.freeze({
  floral: flavorImage('floral', '花香风味氛围'),
  nutty: flavorImage('nutty', '坚果风味氛围'),
  milky: flavorImage('milky', '奶香风味氛围'),
  cocoa: flavorImage('cocoa', '可可风味氛围'),
  tropical: flavorImage('tropical', '热带水果风味氛围'),
  wine: flavorImage('wine', '酒香风味氛围'),
  refreshing: flavorImage('floral', '清爽风味氛围')
})

// V2 商品体系：六风味（花香/坚果/奶香/可可/热带水果/酒香）映射到当前 active 商品。
// 价格取 V2 基础价（MEDIUM_LARGE 商品取中杯）；精品豆统一 50（巴拿马·瑰夏 68）。
// 商品图沿用风味封面营销图（FLAVOR_IMAGES），不替换为商品图。
export const HOME_MENU_PRODUCTS = Object.freeze([
  {
    id: 'ethiopia-yirgacheffe',
    name: '埃塞俄比亚·耶加雪菲',
    originName: 'Yirgacheffe',
    story: '茉莉与柑橘花香，像一杯明亮的花园茶。',
    notes: ['茉莉', '柑橘', '花香'],
    price: '50',
    flavor: 'floral',
    origins: ['埃塞俄比亚'],
    category: 'specialty',
    available: true,
    featured: true,
    displayOrder: 1,
    flavorDirections: ['floral', 'wine'],
    flavorNotes: ['茉莉', '柑橘', '花香'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.floral
  },
  {
    id: 'panama-single-origin',
    name: '巴拿马·瑰夏',
    originName: 'Panama Geisha',
    story: '瑰夏把花香、柑橘与蜂蜜般甜感推向高点，明亮而芳香。',
    notes: ['茉莉', '佛手柑', '蜂蜜'],
    price: '68',
    flavor: 'floral',
    origins: ['巴拿马'],
    category: 'specialty',
    available: true,
    featured: true,
    displayOrder: 2,
    flavorDirections: ['floral', 'tropical'],
    flavorNotes: ['茉莉', '佛手柑', '蜂蜜'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.floral
  },
  {
    id: 'osmanthus-latte',
    name: '拿铁金·桂花特调',
    originName: 'Osmanthus Latte',
    story: '干桂花 x 特制糖浆 x 浓缩，秋意入杯。',
    notes: ['桂花', '花香'],
    price: '38',
    flavor: 'floral',
    origins: [],
    category: 'signature',
    available: true,
    displayOrder: 3,
    flavorDirections: ['floral'],
    flavorNotes: ['桂花', '花香'],
    group: 'signature',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.floral
  },
  {
    id: 'brazil-single-origin',
    name: '巴西·米纳斯',
    originName: 'Brazil',
    story: '坚果与巧克力，醇厚平衡的日常基底。',
    notes: ['坚果', '巧克力', '焦糖'],
    price: '50',
    flavor: 'nutty',
    origins: ['巴西'],
    category: 'specialty',
    available: true,
    featured: true,
    displayOrder: 4,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['坚果', '巧克力', '焦糖'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'colombia-single-origin',
    name: '哥伦比亚·安第斯',
    originName: 'Colombia',
    story: '焦糖与红莓，干净均衡。',
    notes: ['焦糖', '红莓'],
    price: '50',
    flavor: 'nutty',
    origins: ['哥伦比亚'],
    category: 'specialty',
    available: true,
    displayOrder: 5,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['焦糖', '红莓'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'indonesia-mandheling',
    name: '印尼·曼特宁',
    originName: 'Mandheling',
    story: '草本与黑巧克力，厚实饱满。',
    notes: ['草本', '黑巧克力'],
    price: '50',
    flavor: 'nutty',
    origins: ['印度尼西亚'],
    category: 'specialty',
    available: true,
    displayOrder: 6,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['草本', '黑巧克力'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'yunnan-single-origin',
    name: '云南·保山',
    originName: 'Yunnan',
    story: '坚果焦糖与红茶茶感，东方的平衡表达。',
    notes: ['坚果', '焦糖', '茶感'],
    price: '50',
    flavor: 'nutty',
    origins: ['云南'],
    category: 'specialty',
    available: true,
    displayOrder: 7,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['坚果', '焦糖', '茶感'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'americano',
    name: 'Cozy 美式',
    originName: 'Americano',
    story: '以中深烘双拼为基底，黑巧克力与焦糖为主调，收尾干净。',
    notes: ['黑巧克力', '焦糖'],
    price: '22',
    flavor: 'nutty',
    origins: ['巴西'],
    category: 'espresso',
    available: true,
    featured: true,
    displayOrder: 8,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['黑巧克力', '焦糖'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'guatemala-single-origin',
    name: '危地马拉·安提瓜',
    originName: 'Guatemala',
    story: '黑巧克力与香料，沉稳层次。',
    notes: ['黑巧克力', '香料'],
    price: '50',
    flavor: 'cocoa',
    origins: ['危地马拉'],
    category: 'specialty',
    available: true,
    featured: true,
    displayOrder: 9,
    flavorDirections: ['cocoa', 'nutty'],
    flavorNotes: ['黑巧克力', '香料'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.cocoa
  },
  {
    id: 'mocha',
    name: '摩卡',
    originName: 'Mocha',
    story: '比利时黑巧克力酱 x 浓缩 x 鲜奶，顶层鲜奶油。',
    notes: ['黑巧克力', '甜感'],
    price: '32',
    flavor: 'cocoa',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 10,
    flavorDirections: ['cocoa'],
    flavorNotes: ['黑巧克力', '甜感'],
    group: 'classic',
    roastLevel: 'dark',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.cocoa
  },
  {
    id: 'dirty',
    name: 'Cozy Dirty',
    originName: 'Dirty',
    story: '冰博克厚乳 x 热浓缩，冷热交融，大口喝出层次。',
    notes: ['浓郁', '巧克力'],
    price: '32',
    flavor: 'cocoa',
    origins: [],
    category: 'signature',
    available: true,
    displayOrder: 11,
    flavorDirections: ['cocoa'],
    flavorNotes: ['浓郁', '巧克力'],
    group: 'signature',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.cocoa
  },
  {
    id: 'latte',
    name: '经典拿铁',
    originName: 'Caffè Latte',
    story: '鲜牛乳与中深烘双拼融合，奶香与咖香平衡。',
    notes: ['奶香', '咖香'],
    price: '28',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    featured: true,
    displayOrder: 12,
    flavorDirections: ['milky'],
    flavorNotes: ['奶香', '咖香'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'flat-white',
    name: '澳白',
    originName: 'Flat White',
    story: '双份浓缩以薄奶泡融合，咖啡主体更突出，是更「咖啡」的奶咖。',
    notes: ['丝滑', '浓郁'],
    price: '30',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 13,
    flavorDirections: ['milky', 'nutty'],
    flavorNotes: ['丝滑', '浓郁'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'cappuccino',
    name: '卡布奇诺',
    originName: 'Cappuccino',
    story: '绵密厚奶泡与浓缩交融，顶部撒可可粉，口感蓬松。',
    notes: ['可可', '绵密'],
    price: '28',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 14,
    flavorDirections: ['milky', 'cocoa'],
    flavorNotes: ['可可', '绵密'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'coconut-latte',
    name: '生椰拿铁',
    originName: 'Coconut Latte',
    story: '生椰乳 x 浓缩，热带椰香与咖啡甜感。',
    notes: ['椰香', '清爽'],
    price: '32',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 15,
    flavorDirections: ['milky', 'tropical'],
    flavorNotes: ['椰香', '清爽'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'oat-latte',
    name: '燕麦拿铁',
    originName: 'Oat Latte',
    story: '燕麦奶与浓缩融合，谷物香气与焦糖甜感。',
    notes: ['谷物', '顺滑'],
    price: '32',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 16,
    flavorDirections: ['milky', 'nutty'],
    flavorNotes: ['谷物', '顺滑'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'lychee-shake',
    name: '冰摇荔枝咖啡',
    originName: 'Lychee Shake',
    story: '荔枝果香 x 埃塞水洗花香浓缩，冰摇出清爽果感。',
    notes: ['荔枝', '果香'],
    price: '36',
    flavor: 'tropical',
    origins: [],
    category: 'signature',
    available: true,
    displayOrder: 17,
    flavorDirections: ['tropical', 'floral'],
    flavorNotes: ['荔枝', '果香'],
    group: 'signature',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.tropical
  },
  {
    id: 'orange-sparkling',
    name: '柑橘气泡美式',
    originName: 'Orange Sparkling',
    story: '橙皮香气与浓缩以苏打水冰摇，果酸与咖啡的清爽对话。',
    notes: ['柑橘', '气泡'],
    price: '36',
    flavor: 'tropical',
    origins: [],
    category: 'signature',
    available: true,
    displayOrder: 18,
    flavorDirections: ['tropical', 'floral'],
    flavorNotes: ['柑橘', '气泡'],
    group: 'signature',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.tropical
  },
  {
    id: 'kenya-single-origin',
    name: '肯尼亚·涅里',
    originName: 'Kenya',
    story: '黑醋栗与莓果酸质，红酒般余韵。',
    notes: ['黑醋栗', '莓果', '酒香'],
    price: '50',
    flavor: 'wine',
    origins: ['肯尼亚'],
    category: 'specialty',
    available: true,
    featured: true,
    displayOrder: 19,
    flavorDirections: ['wine', 'tropical'],
    flavorNotes: ['黑醋栗', '莓果', '酒香'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.wine
  }
])

export const HOME_FLAVOR_ROUTES = Object.freeze([
  {
    id: 'floral',
    label: '花香',
    originHint: '埃塞 · 巴拿马 · 特调',
    description: '明亮、轻盈，捕捉咖啡中白花与柑橘般的细腻香气。',
    accentColor: '#7B5A3C',
    featuredProductId: 'ethiopia-yirgacheffe',
    productIds: ['ethiopia-yirgacheffe', 'panama-single-origin', 'osmanthus-latte'],
    coverImage: FLAVOR_IMAGES.floral,
    imageAlt: '晨光下的手冲器具与花香场景'
  },
  {
    id: 'nutty',
    label: '坚果',
    originHint: '巴西 · 哥伦比亚 · 印尼',
    description: '温暖而醇厚，呈现坚果、焦糖与巧克力般的甜感。',
    accentColor: '#6B4F3A',
    featuredProductId: 'brazil-single-origin',
    productIds: ['brazil-single-origin', 'colombia-single-origin', 'indonesia-mandheling', 'yunnan-single-origin', 'americano'],
    coverImage: FLAVOR_IMAGES.nutty,
    imageAlt: '胡桃木桌面上的咖啡与坚果'
  },
  {
    id: 'cocoa',
    label: '可可',
    originHint: '危地马拉 · 印尼 · 巴西',
    description: '深沉、绵长，烘焙甜感与可可气息逐渐展开。',
    accentColor: '#5C4033',
    featuredProductId: 'guatemala-single-origin',
    productIds: ['guatemala-single-origin', 'mocha', 'dirty', 'indonesia-mandheling'],
    coverImage: FLAVOR_IMAGES.cocoa,
    imageAlt: '暖色木桌上的摩卡与可可'
  },
  {
    id: 'milky',
    label: '奶香',
    originHint: '经典奶咖',
    description: '柔和、圆润，牛奶与咖啡的经典融合。',
    accentColor: '#8A6A4A',
    featuredProductId: 'latte',
    productIds: ['latte', 'flat-white', 'cappuccino', 'coconut-latte', 'oat-latte'],
    coverImage: FLAVOR_IMAGES.milky,
    imageAlt: '窗边自然光下的拿铁'
  },
  {
    id: 'tropical',
    label: '热带水果',
    originHint: '巴拿马 · 肯尼亚 · 特调',
    description: '明亮酸质与果汁感，来自高海拔产区的成熟果香。',
    accentColor: '#7A5C3E',
    featuredProductId: 'panama-single-origin',
    productIds: ['panama-single-origin', 'kenya-single-origin', 'lychee-shake', 'orange-sparkling'],
    coverImage: FLAVOR_IMAGES.tropical,
    imageAlt: '明亮果香与杯中热带风味'
  },
  {
    id: 'wine',
    label: '酒香',
    originHint: '肯尼亚 · 埃塞 · 哥伦比亚',
    description: '发酵与成熟果实交织，留下复杂而悠长的余韵。',
    accentColor: '#654536',
    featuredProductId: 'kenya-single-origin',
    productIds: ['kenya-single-origin', 'ethiopia-yirgacheffe', 'colombia-single-origin'],
    coverImage: FLAVOR_IMAGES.wine,
    imageAlt: '深色浆果与酒香层次'
  }
])

export const HOME_MENU_SERIES = Object.freeze([
  { id: 'espresso',   name: '经典咖啡', englishName: 'ESPRESSO',   category: 'espresso',   description: '清晰基底', image: productSeriesImage('/images/v2/01-espresso.webp', '经典咖啡'),   href: '/member/order' },
  { id: 'milk',       name: '奶咖',     englishName: 'MILK',       category: 'milk',       description: '柔和圆润', image: productSeriesImage('/images/v2/02-caffe-latte.webp', '奶咖'),       href: '/member/order' },
  { id: 'signature',  name: '招牌特调', englishName: 'SIGNATURE',  category: 'signature',  description: '品牌风味', image: productSeriesImage('/images/v2/03-orange-sparkling.webp', '招牌特调'),       href: '/member/order' },
  { id: 'specialty',  name: '精品咖啡', englishName: 'SPECIALTY',  category: 'specialty',  description: '产地慢萃', image: productSeriesImage('/images/v2/04-origin-ethiopia.webp', '精品咖啡'),  href: '/member/order' },
  { id: 'non-coffee', name: '非咖啡',   englishName: 'NON COFFEE', category: 'non-coffee', description: '无咖啡因', image: productSeriesImage('/images/v2/05-matcha-latte.webp', '非咖啡'),     href: '/member/order' },
  { id: 'bakery',     name: '烘焙轻食', englishName: 'BAKERY',     category: 'bakery',     description: '搭配杯中', image: productSeriesImage('/images/v2/06-basque-cheesecake.webp', '烘焙轻食'), href: '/member/order' }
])

export const FLAVOR_IMAGE_MAP = FLAVOR_IMAGES
