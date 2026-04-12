# 图片与架构图占位清单

把截图放在本目录，文件名与下方保持一致，README 会自动显示。

## 必备（建议优先完成）
- architecture-overview.png：系统总体架构图
- order-sequence.png：下单时序图
- cache-strategy.png：缓存策略图
- order-timeout-zset.png：订单超时取消流程图
- web-order.png：用户端点单页
- member-center.png：会员中心页
- admin-dashboard.png：管理后台看板
- locust-result.png：压测结果图

## 可选补充
- web-checkout.png：结算页（券/积分核销）
- coupon-rules.png：优惠券规则示例页
- points-mall.png：积分商城页
- signin-calendar.png：签到日历页

## 推荐分辨率
- 页面截图：宽度 1400~2000 px
- 架构图：宽度 >= 1800 px
- 压测图：保证关键指标文字清晰可读

## Mermaid 生图建议
1. 在支持 Mermaid 的编辑器中渲染（如 VS Code + Markdown Preview Mermaid）
2. 导出为 PNG/SVG
3. 命名为上方约定文件名
4. 存放到 docs/images
