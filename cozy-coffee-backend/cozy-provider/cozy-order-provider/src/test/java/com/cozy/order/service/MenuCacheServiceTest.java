package com.cozy.order.service;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.cozy.order.service.product.MenuCacheService;
import com.cozy.order.service.product.ProductAddonResolver;
import com.cozy.order.service.converter.OrderDtoConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuCacheServiceTest {

    private MenuCacheService cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        ProductAddonResolver addonResolver = new ProductAddonResolver(
                mock(CoffeeProductAddonGroupMapper.class),
                mock(CoffeeProductAddonMapper.class),
                mock(ProductAddonMapper.class),
                new ObjectMapper());
        OrderDtoConverter dtoConverter = new OrderDtoConverter(new ObjectMapper(), productMapper, addonResolver);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ValueOperations<String, String> stringValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);
        // Redis 菜单缓存持续 miss，强制每次 getMenu() 走 DB 重建路径
        when(valueOps.get(RedisKeyConstants.ORDER_MENU_ACTIVE)).thenReturn(null);
        when(stringValueOps.setIfAbsent(
                eq(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD), anyString(), any(Duration.class)))
                .thenReturn(true);

        when(productMapper.selectList(any())).thenReturn(List.of(product()));

        cacheService = new MenuCacheService(redisTemplate, stringRedisTemplate, productMapper, dtoConverter);
    }

    @Test
    void keepsServingMenuAfterRepeatedRebuilds() {
        // 回归：DB 重建信号量许可必须释放，否则第 5 次重建起 getMenu() 永远返回空菜单。
        for (int i = 0; i < 8; i++) {
            cacheService.invalidate();
            List<CoffeeProductDTO> menu = cacheService.getMenu();
            assertFalse(menu.isEmpty(), "第 " + (i + 1) + " 次重建后菜单不应为空");
        }
    }

    private CoffeeProduct product() {
        CoffeeProduct p = new CoffeeProduct();
        p.setId(1L);
        p.setName("测试咖啡");
        p.setStatus("active");
        p.setCategory("espresso");
        p.setPrice(new BigDecimal("20.00"));
        return p;
    }
}
