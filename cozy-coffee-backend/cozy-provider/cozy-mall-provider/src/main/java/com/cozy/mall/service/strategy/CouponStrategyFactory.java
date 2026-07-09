package com.cozy.mall.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券策略工厂（Phase 6）。
 * 自动发现所有 CouponStrategy Bean，按 supportedType() 注册。
 * 新增券类型 = 新增 Strategy Bean，工厂自动注册。
 */
@Component
@RequiredArgsConstructor
public class CouponStrategyFactory {

    private final Map<String, CouponStrategy> strategyMap;

    public CouponStrategyFactory(List<CouponStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(CouponStrategy::supportedType, Function.identity()));
    }

    public CouponStrategy getStrategy(String couponType) {
        CouponStrategy strategy = strategyMap.get(couponType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的券类型: " + couponType);
        }
        return strategy;
    }
}
