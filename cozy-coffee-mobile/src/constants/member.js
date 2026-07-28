export const MEMBER_LEVELS = Object.freeze(['basic', 'silver', 'gold', 'diamond', 'black'])

export const MEMBER_LEVEL_THRESHOLDS = Object.freeze({
  basic: 0,
  silver: 500,
  gold: 1500,
  diamond: 4000,
  black: 9000
})

export const MEMBER_LEVEL_NAMES = Object.freeze({
  basic: '基础会员',
  silver: '白银会员',
  gold: '黄金会员',
  diamond: '钻石会员',
  black: '黑金会员'
})

export function getMemberLevelName(level) {
  return MEMBER_LEVEL_NAMES[level] || '注册会员'
}

// 会员等级主题映射：surface（卡背景，可纯色可渐变）、text（文字色）、accent（进度条/徽章）、isDark（暗背景标记）
// CSS 变量在卡片级通过 :style 注入（见 useMemberTheme），不暴露全局 :root
export const MEMBER_LEVEL_THEMES = Object.freeze({
  basic: {
    surface: '#F7F5F3',
    text: '#2B1E16',
    accent: '#8D6E63',
    isDark: false
  },
  silver: {
    surface: 'linear-gradient(135deg, #ECE8E4, #D6D0CA)',
    text: '#3E342E',
    accent: '#8C7B70',
    isDark: false
  },
  gold: {
    surface: 'linear-gradient(135deg, #E8D7A5, #C9A45C)',
    text: '#3B2920',
    accent: '#B8862D',
    isDark: false
  },
  diamond: {
    surface: 'linear-gradient(135deg, #DCE4E6, #B8C6CA)',
    text: '#263238',
    accent: '#546E7A',
    isDark: false
  },
  black: {
    surface: '#171411',
    text: '#E6C97A',
    accent: '#C9A227',
    isDark: true
  }
})

// dev 测试覆盖用的 mock memberInfo，尽量接近 MemberDTO 形状
export const DEV_MEMBER_MOCK = Object.freeze({
  basic:   { id: 90001, memberLevel: 'basic',   levelName: '基础会员', expTotal: 100,  currentPoints: 50,    couponCount: 0, exchangeCouponCount: 0 },
  silver:  { id: 90002, memberLevel: 'silver',  levelName: '白银会员', expTotal: 800,  currentPoints: 1200,  couponCount: 2, exchangeCouponCount: 1 },
  gold:    { id: 90003, memberLevel: 'gold',    levelName: '黄金会员', expTotal: 2000, currentPoints: 3600,  couponCount: 3, exchangeCouponCount: 2 },
  diamond: { id: 90004, memberLevel: 'diamond', levelName: '钻石会员', expTotal: 4500, currentPoints: 8888,  couponCount: 5, exchangeCouponCount: 3 },
  black:   { id: 90005, memberLevel: 'black',   levelName: '黑金会员', expTotal: 9500, currentPoints: 18888, couponCount: 8, exchangeCouponCount: 5 }
})

