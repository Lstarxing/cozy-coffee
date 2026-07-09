export const MEMBER_LEVEL = {
  BASIC: 'basic',
  SILVER: 'silver',
  GOLD: 'gold',
  DIAMOND: 'diamond',
  BLACK: 'black'
}

export const MEMBER_LEVEL_MAP = {
  [MEMBER_LEVEL.BASIC]: { label: '基础 Classic', color: '#9CA3AF', minExp: 0 },
  [MEMBER_LEVEL.SILVER]: { label: '白银 Silver', color: '#A8B8C8', minExp: 500 },
  [MEMBER_LEVEL.GOLD]: { label: '黄金 Gold', color: '#D4A853', minExp: 1500 },
  [MEMBER_LEVEL.DIAMOND]: { label: '钻石 Platinum', color: '#7B9EC2', minExp: 4000 },
  [MEMBER_LEVEL.BLACK]: { label: '黑金 Black', color: '#1F1F1F', minExp: 9000 }
}
