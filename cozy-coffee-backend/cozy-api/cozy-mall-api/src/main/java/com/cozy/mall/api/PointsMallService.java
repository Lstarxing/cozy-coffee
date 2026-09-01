package com.cozy.mall.api;

import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.request.RedeemRequest;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.mall.dto.response.CouponUsageResult;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.mall.dto.response.PointsRefundDeadLetterDTO;
import com.cozy.mall.dto.response.PointsProductDTO;
import com.cozy.mall.dto.response.UserCouponDTO;
import java.math.BigDecimal;
import java.util.List;

public interface PointsMallService {
        /**
         * 获取上架商品列表
         *
         * @param userId 当前用户ID（可选，用于查询月度限购进度），未登录传 null
         */
        List<PointsProductDTO> listActiveProducts(Long userId);

        PointsProductDTO getProduct(Long id);

        PointsOrderDTO redeem(Long userId, RedeemRequest request);

        List<PointsOrderDTO> listUserOrders(Long userId);

        PointsOrderDTO getOrder(Long orderId, Long userId);

        /**
         * 取消订单（仅限发货前）
         *
         * @param orderId 订单ID
         * @param userId  用户ID
         * @return 取消后的订单信息
         */
        PointsOrderDTO cancelOrder(Long orderId, Long userId);

        // ==================== 用户券包 ====================

        /**
         * 获取用户券包（所有券）
         */
        List<UserCouponDTO> getUserCoupons(Long userId, String status);

        /**
         * 获取下单可用券
         *
         * @param userId      用户ID
         * @param orderAmount 订单金额
         * @return 可用券列表
         */
        List<UserCouponDTO> getAvailableCoupons(Long userId, BigDecimal orderAmount);

        /**
         * 获取下单可用券（支持商品明细检查）
         *
         * @param userId      用户ID
         * @param orderAmount 订单金额
         * @param items       商品列表
         * @return 可用券列表
         */
        List<UserCouponDTO> getAvailableCoupons(Long userId, BigDecimal orderAmount,
                        List<ItemCheckDTO> items);

        /**
         * 使用券（下单时核销）- 旧版兼容
         *
         * @param userId      用户ID
         * @param couponCode  券码
         * @param orderAmount 订单金额
         * @return 折扣金额
         */
        BigDecimal useCoupon(Long userId, String couponCode, BigDecimal orderAmount);

        /**
         * 使用券（下单时核销）- 新版，返回券类型
         * 用于判断是否应该发放积分（兑换券不发放积分）
         *
         * @param userId      用户ID
         * @param couponCode  券码
         * @param orderAmount 订单金额
         * @param items       订单中的商品详情列表（用于精确计算折扣）
         * @return 券核销结果（包含折扣金额和券类型）
         */
        CouponUsageResult useCouponWithResult(Long userId, String couponCode, BigDecimal orderAmount,
                        List<ItemCheckDTO> items);

        /**
         * Calculate and validate a coupon without consuming it. Checkout preview must use this method.
         */
        CouponUsageResult previewCouponWithResult(Long userId, String couponCode, BigDecimal orderAmount,
                        List<ItemCheckDTO> items);

        /**
         * 整组券预览（不消费）：组合校验（主券≤1 / 独占唯一 / 配送费券上限 / SHOT 杯数 / 重复）+ 统一计算。
         * 主券基数 couponBase（商品小计−会员折扣，不含加料）；辅券基数 couponBase+addonsTotal；
         * addonPrices 供尊享通兑券 freeAddon 免加料取最高 N 个。
         * 与 useCouponCombination 共用同一套组合校验与计算。
         */
        CouponCombinationResult previewCouponCombination(Long userId, List<String> couponCodes,
                        BigDecimal couponBase, BigDecimal addonsTotal, List<BigDecimal> addonPrices,
                        List<ItemCheckDTO> items);

        /**
         * 整组券核销（下单用）：组合校验 + 计算 + 整组冻结（ISSUED→FROZEN）。
         * 订单落库失败时由 publishCouponRollbackEvent 整组回滚。
         */
        CouponCombinationResult useCouponCombination(Long userId, List<String> couponCodes,
                        BigDecimal couponBase, BigDecimal addonsTotal, List<BigDecimal> addonPrices,
                        List<ItemCheckDTO> items);

        // ==================== 管理端方法 ====================

        /**
         * 获取所有兑换订单（管理端）
         */
        List<PointsOrderDTO> listAllOrders(String status);

        /**
         * 更新订单状态（管理端）
         */
        PointsOrderDTO updateOrderStatus(Long orderId, String status);

        /**
         * 更新物流信息（管理端）
         */
        PointsOrderDTO updateShipping(Long orderId, String company, String trackingNo);

        // ==================== 积分商品管理（管理端）====================

        /**
         * 获取所有积分商品（管理端，含下架商品）
         */
        List<PointsProductDTO> listAllProducts();

        /**
         * 添加积分商品
         */
        PointsProductDTO addProduct(PointsProductDTO product);

        /**
         * 更新积分商品
         */
        PointsProductDTO updateProduct(Long productId, PointsProductDTO product);

        /**
         * 删除积分商品
         */
        void deleteProduct(Long productId);

        /**
         * 切换商品上下架状态
         */
        PointsProductDTO toggleProductStatus(Long productId);

        /**
         * 获取兑换订单详情（管理端）
         */
        PointsOrderDTO getRedemptionDetail(Long orderId);

        /**
         * 用户确认收货（快递订单）
         */
        PointsOrderDTO confirmReceipt(Long orderId, Long userId);

        /**
         * 删除兑换订单（管理端，用于清理脏数据）
         */
        void deleteOrder(Long orderId);

        /** 查询积分退款 DEAD outbox（管理端运维）。 */
        List<PointsRefundDeadLetterDTO> listDeadPointRefunds(Integer limit);

        /** DEAD → PENDING 的 CAS 人工重试；记录操作人和重试次数。 */
        void retryDeadPointRefund(Long id, Long operatorId);

        /** 当前积分退款 DEAD outbox 数量。 */
        long countDeadPointRefunds();

        /**
         * 回滚/归还优惠券（取消订单时调用）
         *
         * @param couponId 券ID
         * @param userId   用户ID
         */
        void rollbackCoupon(Long couponId, Long userId);

        /** 按事件幂等键整组回滚订单券；同一事件重复投递只处理一次。 */
        void rollbackCoupons(String rollbackEventId, Long orderId, Long userId,
                        Long mainCouponId, List<Long> addonCouponIds);

        /**
         * 确认优惠券（订单支付/接单成功后调用）：FROZEN → USED
         *
         * @param couponId 券ID
         * @param userId   用户ID
         */
        void confirmCoupon(Long couponId, Long userId);

        /** 整组确认优惠券（主券 + 附加券），在 mall 单事务内完成。 */
        void confirmCoupons(List<Long> couponIds, Long userId);

        /**
         * 按券 ID 批量查询优惠券（订单详情展示券名用）
         *
         * @param couponIds 券ID列表
         * @return 优惠券 DTO 列表
         */
        List<UserCouponDTO> getCouponsByIds(List<Long> couponIds);

        /**
         * v5.0: 发放优惠券给用户（用于签到奖励等场景）
         *
         * @param userId         用户ID
         * @param couponType     券类型（如 SIGNIN_7DAY）
         * @param uniqueKey      唯一标识（用于幂等性检查）
         * @param minAmount      使用门槛金额
         * @param discountAmount 优惠金额
         * @param validDays      有效天数
         */
        void issueCouponToUser(Long userId, String couponType, String uniqueKey, double minAmount,
                        double discountAmount, int validDays);

        /**
         * v5.3: 发放带 SKU 限制的优惠券
         *
         * @param userId         用户ID
         * @param couponType     券类型
         * @param uniqueKey      唯一标识（用于幂等性检查）
         * @param minAmount      使用门槛金额
         * @param discountAmount 优惠金额
         * @param validDays      有效天数
         * @param extraRuleJson  额外规则JSON（如 {"skuLimit":"STANDARD_ONLY","categoryBlocklist":["soe"]}）
         */
        void issueCouponWithSkuLimit(Long userId, String couponType, String uniqueKey, double minAmount,
                        double discountAmount, int validDays, String extraRuleJson);

        /**
         * v5.2: 发放新用户首单五折券
         * 有效期 7 天，全场饮品可用，不叠加其他优惠
         *
         * @param userId 用户ID
         */
        void issueNewUserCoupon(Long userId);
}
