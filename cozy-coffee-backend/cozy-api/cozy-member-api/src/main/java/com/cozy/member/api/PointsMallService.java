package com.cozy.member.api;

import com.cozy.member.dto.request.RedeemRequest;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import java.util.List;

public interface PointsMallService {
    List<PointsProductDTO> listActiveProducts();

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
}
