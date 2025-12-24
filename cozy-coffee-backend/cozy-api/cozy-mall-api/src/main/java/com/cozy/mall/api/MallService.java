package com.cozy.mall.api;

import com.cozy.mall.dto.request.RedeemRequest;
import com.cozy.mall.dto.response.PointsProductDTO;
import com.cozy.mall.dto.response.PointsOrderDTO;

import java.util.List;

/**
 * 积分商城服务接口 - 独立微服务
 */
public interface MallService {
    
    /**
     * 获取积分商城商品列表
     */
    List<PointsProductDTO> listProducts();
    
    /**
     * 获取商品详情
     */
    PointsProductDTO getProduct(Long productId);
    
    /**
     * 兑换商品
     * @param userId 用户ID
     * @param currentPoints 用户当前积分
     * @param request 兑换请求
     * @return 兑换订单
     */
    PointsOrderDTO redeemProduct(Long userId, Integer currentPoints, RedeemRequest request);
    
    /**
     * 获取用户兑换订单列表
     */
    List<PointsOrderDTO> listUserOrders(Long userId);
    
    /**
     * 取消订单
     */
    boolean cancelOrder(Long orderId, Long userId);
}
