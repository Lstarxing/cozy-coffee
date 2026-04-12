# 图片与架构图占位清单

把截图放在本目录下的分类目录中，文件名与下方保持一致，README 会自动显示。

## 目录结构建议
- frontend-web：用户端界面
- frontend-admin：管理后台界面
- frontend-mobile：移动端界面（可选）
- frontend-common：压测结果、通用截图

## 必备（建议优先完成）
- architecture-overview.png：系统总体架构图
- order-sequence.png：下单时序图
- cache-strategy.png：缓存策略图
- order-timeout-zset.png：订单超时取消流程图

## 前端截图（与 README_GITHUB_SHOWCASE.md 对齐）
- frontend-web/01-menu.png：用户端-首页/菜单
- frontend-web/02-cart.png：用户端-购物车
- frontend-web/03-checkout.png：用户端-结算页
- frontend-web/04-order-success.png：用户端-下单成功
- frontend-web/05-member-center.png：会员中心
- frontend-web/06-signin-calendar.png：签到日历
- frontend-web/07-points-mall.png：积分商城
- frontend-web/08-coupon-selector.png：优惠券选择弹窗
- frontend-admin/01-order-list.png：后台-实时订单列表
- frontend-admin/02-order-detail.png：后台-订单详情
- frontend-admin/03-dashboard.png：后台-运营看板
- frontend-common/01-locust-result.png：压测结果图

## 可选补充
- frontend-mobile/01-menu.png：移动端菜单页
- frontend-mobile/02-cart.png：移动端购物车页
- frontend-mobile/03-checkout.png：移动端结算页
- frontend-common/02-sse-realtime.png：后台实时推送示例
- frontend-common/03-error-fallback.png：异常兜底页面

## 推荐分辨率
- 页面截图：宽度 1400~2000 px
- 架构图：宽度 >= 1800 px
- 压测图：保证关键指标文字清晰可读

## Mermaid 生图建议
1. 在支持 Mermaid 的编辑器中渲染（如 VS Code + Markdown Preview Mermaid）
2. 导出为 PNG/SVG
3. 命名为上方约定文件名
4. 存放到 docs/images 根目录（架构图）或对应前端子目录
