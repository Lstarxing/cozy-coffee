const MENU_IMAGE_BASE = 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/menu'

function flavorImage(flavor) {
  return Object.freeze({
    avif: [640, 960, 1200].map(width => `${MENU_IMAGE_BASE}/${flavor}-${width}.avif ${width}w`).join(', '),
    webp: [640, 960, 1200].map(width => `${MENU_IMAGE_BASE}/${flavor}-${width}.webp ${width}w`).join(', '),
    jpg: [640, 960, 1200].map(width => `${MENU_IMAGE_BASE}/${flavor}-${width}.jpg ${width}w`).join(', '),
    fallback: `${MENU_IMAGE_BASE}/${flavor}-960.jpg`
  })
}

const FLAVOR_IMAGES = Object.freeze({
  floral: flavorImage('floral'),
  nutty: flavorImage('nutty'),
  milky: flavorImage('milky'),
  cocoa: flavorImage('cocoa'),
  refreshing: flavorImage('refreshing')
})

export const HOME_FLAVOR_ROUTES = Object.freeze([
  {
    id: 'floral',
    label: '花香',
    coverImage: FLAVOR_IMAGES.floral,
    imageAlt: '晨光下的手冲器具、白陶杯与柑橘花香场景',
    description: '明亮、轻盈，寻找咖啡原产地最初的香气。',
    featured: {
      productId: 'yunnan-soe',
      story: '来自云南保山高海拔庄园，轻烘焙保留花香与柑橘调性。'
    },
    menu: {
      title: '单一产地',
      productIds: ['yunnan-soe', 'ethiopia-yirgacheffe', 'panama-single-origin']
    }
  },
  {
    id: 'nutty',
    label: '坚果',
    coverImage: FLAVOR_IMAGES.nutty,
    imageAlt: '胡桃木桌面上的咖啡、核桃与铜制手冲壶',
    description: '温暖、醇厚，适合每天开始的一杯。',
    featured: {
      productId: 'americano',
      story: '以巴西拼配为基底，坚果、可可与焦糖甜感保持稳定平衡。'
    },
    menu: {
      title: '平衡拼配',
      productIds: ['americano', 'flat-white', 'colombia-single-origin']
    }
  },
  {
    id: 'milky',
    label: '奶香',
    coverImage: FLAVOR_IMAGES.milky,
    imageAlt: '窗边自然光下的拿铁、牛奶壶与早餐桌',
    description: '柔和、平衡，牛奶与咖啡的经典融合。',
    featured: {
      productId: 'latte',
      story: '浓缩与鲜奶自然融合，让焦糖甜感与柔和奶香留在杯中。'
    },
    menu: {
      title: '奶咖表达',
      productIds: ['latte', 'cappuccino', 'coconut-latte']
    }
  },
  {
    id: 'cocoa',
    label: '可可',
    coverImage: FLAVOR_IMAGES.cocoa,
    imageAlt: '暖色咖啡馆木桌上的摩卡、黑巧与可可粉',
    description: '深沉、绵长，让可可与烘焙香气缓慢展开。',
    featured: {
      productId: 'mocha',
      story: '黑巧克力与深烘浓缩相互托住，形成厚实而克制的甜感。'
    },
    menu: {
      title: '深烘表达',
      productIds: ['mocha', 'cappuccino', 'americano']
    }
  },
  {
    id: 'refreshing',
    label: '清爽',
    coverImage: FLAVOR_IMAGES.refreshing,
    imageAlt: '阳光与绿植旁的透明玻璃冰咖啡',
    description: '清透、明快，在阳光与冰感之间寻找轻盈收口。',
    featured: {
      productId: 'coconut-latte',
      story: '清爽椰香与浓缩自然衔接，保留轻盈甜感和干净余韵。'
    },
    menu: {
      title: '清爽选择',
      productIds: ['coconut-latte', 'panama-single-origin', 'americano']
    }
  }
])

export const HOME_MENU_PRODUCTS = Object.freeze([
  { id: 'yunnan-soe', name: '云南 SOE', group: 'single-origin', flavorDirections: ['floral'], roastLevel: 'light', brewMethod: 'pour-over', flavorNotes: ['花香', '柑橘', '茶感'], price: '38', available: true, displayOrder: 1 },
  { id: 'americano', name: '美式', group: 'classic', flavorDirections: ['nutty', 'cocoa', 'refreshing'], roastLevel: 'medium', brewMethod: 'espresso', flavorNotes: ['坚果', '可可'], price: '18', available: true, displayOrder: 1 },
  { id: 'latte', name: '拿铁', group: 'classic', flavorDirections: ['milky'], roastLevel: 'medium', brewMethod: 'espresso', flavorNotes: ['奶香', '焦糖'], price: '25', available: true, displayOrder: 2 },
  { id: 'flat-white', name: '澳白', group: 'classic', flavorDirections: ['milky', 'nutty'], roastLevel: 'medium', brewMethod: 'espresso', flavorNotes: ['丝滑', '浓郁'], price: '28', available: true, displayOrder: 3 },
  { id: 'cappuccino', name: '卡布奇诺', group: 'classic', flavorDirections: ['milky', 'cocoa'], roastLevel: 'medium', brewMethod: 'espresso', flavorNotes: ['可可', '绵密'], price: '30', available: true, displayOrder: 4 },
  { id: 'coconut-latte', name: '生椰拿铁', group: 'classic', flavorDirections: ['milky', 'refreshing'], roastLevel: 'medium', brewMethod: 'espresso', flavorNotes: ['清爽', '椰香'], price: '28', available: true, displayOrder: 5 },
  { id: 'mocha', name: '摩卡', group: 'classic', flavorDirections: ['cocoa'], roastLevel: 'dark', brewMethod: 'espresso', flavorNotes: ['巧克力', '甜感'], price: '30', available: true, displayOrder: 6 },
  { id: 'ethiopia-yirgacheffe', name: '耶加雪菲', group: 'single-origin', flavorDirections: ['floral'], roastLevel: 'light', brewMethod: 'pour-over', flavorNotes: ['白花', '柑橘'], price: '45', available: true, displayOrder: 2 },
  { id: 'colombia-single-origin', name: '哥伦比亚', group: 'single-origin', flavorDirections: ['nutty'], roastLevel: 'medium', brewMethod: 'pour-over', flavorNotes: ['焦糖', '红果'], price: '42', available: true, displayOrder: 3 },
  { id: 'panama-single-origin', name: '巴拿马', group: 'single-origin', flavorDirections: ['floral', 'refreshing'], roastLevel: 'light', brewMethod: 'pour-over', flavorNotes: ['热带水果', '茶感'], price: '68', available: true, displayOrder: 4 }
])
