const MENU_IMAGE_BASE = import.meta.env.VITE_MENU_IMAGE_BASE || '/images/home/menu'
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

function seriesImage(series, alt = '') {
  const base = MENU_IMAGE_BASE
  const name = `menu-series-${series}`
  return Object.freeze({
    avif: `${base}/${name}-800.avif`,
    webp: `${base}/${name}-800.webp`,
    jpg: `${base}/${name}-800.jpg`,
    fallback: `${base}/${name}-800.jpg`,
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

export const HOME_MENU_PRODUCTS = Object.freeze([
  {
    id: 'yunnan-soe',
    name: '云南 SOE',
    originName: 'Yunnan SOE',
    story: '来自云南高海拔庄园，明亮花香与柑橘交织，干净而持久。',
    notes: ['花香', '柑橘', '茉莉'],
    price: '38',
    flavor: 'floral',
    origins: ['云南'],
    category: 'pour-over',
    available: true,
    featured: true,
    displayOrder: 1,
    flavorDirections: ['floral', 'wine'],
    flavorNotes: ['花香', '柑橘', '茉莉'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.floral
  },
  {
    id: 'ethiopia-yirgacheffe',
    name: '耶加雪菲',
    originName: 'Yirgacheffe',
    story: '经典水洗耶加，白花与柑橘的清透层次，茶感收口。',
    notes: ['茉莉', '柠檬', '蜂蜜'],
    price: '45',
    flavor: 'floral',
    origins: ['埃塞俄比亚'],
    category: 'pour-over',
    available: true,
    displayOrder: 2,
    flavorDirections: ['floral', 'tropical'],
    flavorNotes: ['茉莉', '柠檬', '蜂蜜'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.floral
  },
  {
    id: 'ethiopia-natural',
    name: '埃塞俄比亚 日晒',
    originName: 'Ethiopia Natural',
    story: '日晒带来饱满果香与酒感余韵，甜感更直接。',
    notes: ['花香', '蓝莓', '茶感'],
    price: '48',
    flavor: 'tropical',
    origins: ['埃塞俄比亚'],
    category: 'pour-over',
    available: true,
    displayOrder: 3,
    flavorDirections: ['floral', 'tropical', 'wine'],
    flavorNotes: ['花香', '蓝莓', '茶感'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.tropical
  },
  {
    id: 'kenya-single-origin',
    name: '肯尼亚 AA',
    originName: 'Kenya AA',
    story: '高海拔双重水洗，黑加仑与莓果骨架清晰有张力。',
    notes: ['黑加仑', '莓果', '酒香'],
    price: '48',
    flavor: 'wine',
    origins: ['肯尼亚'],
    category: 'pour-over',
    available: true,
    displayOrder: 4,
    flavorDirections: ['tropical', 'wine'],
    flavorNotes: ['黑加仑', '莓果', '酒香'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.wine
  },
  {
    id: 'brazil-single-origin',
    name: '巴西',
    originName: 'Brazil',
    story: '坚果、可可与焦糖甜感稳定，适合作为日常基底。',
    notes: ['坚果', '巧克力', '焦糖'],
    price: '36',
    flavor: 'nutty',
    origins: ['巴西'],
    category: 'pour-over',
    available: true,
    displayOrder: 5,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['坚果', '巧克力', '焦糖'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'colombia-single-origin',
    name: '哥伦比亚',
    originName: 'Colombia',
    story: '安第斯山脉的甜感与平衡酸质，在同一条清晰风味轴线上。',
    notes: ['焦糖', '坚果', '红果'],
    price: '42',
    flavor: 'nutty',
    origins: ['哥伦比亚'],
    category: 'pour-over',
    available: true,
    featured: true,
    displayOrder: 6,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['焦糖', '坚果', '红果'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'guatemala-single-origin',
    name: '危地马拉',
    originName: 'Guatemala',
    story: '火山土壤赋予扎实甜感，尾韵留下细致的可可与香料。',
    notes: ['坚果', '可可', '香料'],
    price: '44',
    flavor: 'cocoa',
    origins: ['危地马拉'],
    category: 'pour-over',
    available: true,
    featured: true,
    displayOrder: 7,
    flavorDirections: ['cocoa', 'nutty'],
    flavorNotes: ['坚果', '可可', '香料'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.cocoa
  },
  {
    id: 'panama-single-origin',
    name: '巴拿马瑰夏',
    originName: 'Panama Geisha',
    story: '瑰夏把花香、柑橘与蜂蜜般甜感推向高点，芳香记忆清晰。',
    notes: ['茉莉', '热带水果', '蜂蜜'],
    price: '68',
    flavor: 'tropical',
    origins: ['巴拿马'],
    category: 'pour-over',
    available: true,
    featured: true,
    displayOrder: 8,
    flavorDirections: ['floral', 'tropical'],
    flavorNotes: ['茉莉', '热带水果', '蜂蜜'],
    group: 'single-origin',
    roastLevel: 'light',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.tropical
  },
  {
    id: 'indonesia-mandheling',
    name: '印尼曼特宁',
    originName: 'Mandheling',
    story: '湿刨处理带来低沉香料感与厚重质地，余韵绵长。',
    notes: ['黑巧克力', '草本', '香料'],
    price: '40',
    flavor: 'nutty',
    origins: ['印度尼西亚'],
    category: 'pour-over',
    available: true,
    displayOrder: 9,
    flavorDirections: ['cocoa'],
    flavorNotes: ['黑巧克力', '草本', '香料'],
    group: 'single-origin',
    roastLevel: 'medium',
    brewMethod: 'pour-over',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'americano',
    name: '美式',
    originName: 'Americano',
    story: '以巴西拼配为基底，坚果与可可保持稳定平衡。',
    notes: ['坚果', '可可'],
    price: '18',
    flavor: 'nutty',
    origins: ['巴西'],
    category: 'espresso',
    available: true,
    displayOrder: 10,
    flavorDirections: ['nutty', 'cocoa'],
    flavorNotes: ['坚果', '可可'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.nutty
  },
  {
    id: 'latte',
    name: '拿铁',
    originName: 'Latte',
    story: '浓缩与鲜奶自然融合，焦糖甜感与柔和奶香留在杯中。',
    notes: ['奶香', '焦糖'],
    price: '25',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    featured: true,
    displayOrder: 11,
    flavorDirections: ['milky'],
    flavorNotes: ['奶香', '焦糖'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'flat-white',
    name: '澳白',
    originName: 'Flat White',
    story: '更浓缩的比例，丝滑口感与坚果甜感并存。',
    notes: ['丝滑', '浓郁'],
    price: '28',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 12,
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
    story: '绵密奶泡托住可可与烘焙香气，结构清晰。',
    notes: ['可可', '绵密'],
    price: '30',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 13,
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
    story: '清爽椰香与浓缩自然衔接，轻盈甜感与干净余韵。',
    notes: ['清爽', '椰香'],
    price: '28',
    flavor: 'milky',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 14,
    flavorDirections: ['milky', 'tropical'],
    flavorNotes: ['清爽', '椰香'],
    group: 'classic',
    roastLevel: 'medium',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.milky
  },
  {
    id: 'mocha',
    name: '摩卡',
    originName: 'Mocha',
    story: '黑巧克力与深烘浓缩相互托住，形成厚实而克制的甜感。',
    notes: ['巧克力', '甜感'],
    price: '30',
    flavor: 'cocoa',
    origins: [],
    category: 'milk',
    available: true,
    displayOrder: 15,
    flavorDirections: ['cocoa'],
    flavorNotes: ['巧克力', '甜感'],
    group: 'classic',
    roastLevel: 'dark',
    brewMethod: 'espresso',
    heroImage: FLAVOR_IMAGES.cocoa
  }
])

export const HOME_FLAVOR_ROUTES = Object.freeze([
  {
    id: 'floral',
    label: '花香',
    originHint: '云南 · 埃塞 · 巴拿马',
    description: '明亮、轻盈，捕捉咖啡中白花与柑橘般的细腻香气。',
    accentColor: '#7B5A3C',
    featuredProductId: 'yunnan-soe',
    productIds: ['yunnan-soe', 'ethiopia-yirgacheffe', 'panama-single-origin', 'ethiopia-natural'],
    coverImage: FLAVOR_IMAGES.floral,
    imageAlt: '晨光下的手冲器具与花香场景'
  },
  {
    id: 'nutty',
    label: '坚果',
    originHint: '巴西 · 哥伦比亚 · 危地马拉',
    description: '温暖而醇厚，呈现坚果、焦糖与巧克力般的甜感。',
    accentColor: '#6B4F3A',
    featuredProductId: 'brazil-single-origin',
    productIds: ['brazil-single-origin', 'colombia-single-origin', 'guatemala-single-origin'],
    coverImage: FLAVOR_IMAGES.nutty,
    imageAlt: '胡桃木桌面上的咖啡与坚果'
  },
  {
    id: 'cocoa',
    label: '可可',
    originHint: '危地马拉 · 巴西 · 印尼',
    description: '深沉、绵长，烘焙甜感与可可气息逐渐展开。',
    accentColor: '#5C4033',
    featuredProductId: 'guatemala-single-origin',
    productIds: ['guatemala-single-origin', 'brazil-single-origin', 'colombia-single-origin', 'indonesia-mandheling'],
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
    productIds: ['latte', 'flat-white', 'cappuccino', 'coconut-latte'],
    coverImage: FLAVOR_IMAGES.milky,
    imageAlt: '窗边自然光下的拿铁'
  },
  {
    id: 'tropical',
    label: '热带水果',
    originHint: '巴拿马 · 埃塞 · 肯尼亚',
    description: '明亮酸质与果汁感，来自高海拔产区的成熟果香。',
    accentColor: '#7A5C3E',
    featuredProductId: 'panama-single-origin',
    productIds: ['panama-single-origin', 'ethiopia-natural', 'kenya-single-origin', 'ethiopia-yirgacheffe'],
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
    productIds: ['kenya-single-origin', 'ethiopia-natural', 'colombia-single-origin'],
    coverImage: FLAVOR_IMAGES.wine,
    imageAlt: '深色浆果与酒香层次'
  }
])

export const HOME_MENU_SERIES = Object.freeze([
  { id: 'espresso',  name: '意式浓缩',    englishName: 'ESPRESSO',    category: 'espresso',  description: '清晰基底', image: seriesImage('espresso',  '意式浓缩系列'), href: '/member/order' },
  { id: 'milk',      name: '奶咖系列',    englishName: 'MILK',        category: 'milk',      description: '柔和圆润', image: seriesImage('milk',      '奶咖系列'),     href: '/member/order' },
  { id: 'cold-brew', name: '冷萃系列',    englishName: 'COLD BREW',   category: 'cold-brew', description: '清爽甘甜', image: seriesImage('cold-brew', '冷萃系列'),     href: '/member/order' },
  { id: 'pour-over', name: '手冲系列',    englishName: 'POUR OVER',   category: 'pour-over', description: '慢萃风味', image: seriesImage('pour-over', '手冲系列'),     href: '/member/order' },
  { id: 'seasonal',  name: '季节限定',    englishName: 'SEASONAL',    category: 'seasonal',  description: '时令之选', image: seriesImage('tea',       '季节限定'),     href: '/member/order' },
  { id: 'dessert',   name: '烘焙甜品',    englishName: 'PASTRY',      category: 'dessert',   description: '搭配杯中', image: seriesImage('dessert',   '烘焙甜品'),     href: '/member/order' }
])

export const FLAVOR_IMAGE_MAP = FLAVOR_IMAGES
