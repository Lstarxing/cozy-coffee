package com.cozy.order.service;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeBeanMapper;
import com.cozy.order.mapper.CoffeeBlendMapper;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.cozy.order.service.product.MenuCacheService;
import com.cozy.order.service.product.ProductAddonResolver;
import com.cozy.order.service.product.ProductRuleValidator;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuCacheServiceTest {

    private MenuCacheService cacheService;
    private CoffeeProductMapper productMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        productMapper = mock(CoffeeProductMapper.class);
        ProductAddonResolver addonResolver = new ProductAddonResolver(
                mock(CoffeeProductAddonGroupMapper.class),
                mock(CoffeeProductAddonMapper.class),
                mock(ProductAddonMapper.class),
                new ObjectMapper());
        OrderDtoConverter dtoConverter = new OrderDtoConverter(new ObjectMapper(), productMapper, addonResolver,
                new ProductRuleValidator(), mock(CoffeeBeanMapper.class), mock(CoffeeBlendMapper.class));

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

    @Test
    void ordersMenuByCategoryThenSortOrderWithSpecialtyComboLast() {
        // 回归：分类展示顺序（01经典→06烘焙）> 精品固定组合置尾 > 分类内 sortOrder。
        // 打乱输入顺序，验证菜单仍按展示顺序输出（sort_order 是分类内计数器，不能跨分类全局排序）。
        List<CoffeeProduct> shuffled = List.of(
                product("BAKERY", "06-oat-cookie", 1, null),
                product("MILK", "02-caffe-latte", 3, null),
                product("ESPRESSO", "01-espresso", 2, null),
                product("SPECIALTY", "04-origin-ethiopia", 4, null),
                product("SPECIALTY", "04-one-bean-two", 2, "FIXED_COMBINATION"),
                product("SPECIALTY", "04-one-bean-three", 3, "FIXED_COMBINATION"),
                product("ESPRESSO", "01-americano", 1, null));
        when(productMapper.selectList(any())).thenReturn(shuffled);

        List<String> codes = cacheService.getMenu().stream()
                .map(CoffeeProductDTO::getProductCode)
                .collect(Collectors.toList());

        assertEquals(List.of(
                "01-americano", "01-espresso",
                "02-caffe-latte",
                "04-origin-ethiopia", "04-one-bean-two", "04-one-bean-three",
                "06-oat-cookie"), codes);
    }

    private CoffeeProduct product() {
        return product("espresso", "测试咖啡", 1, null);
    }

    private CoffeeProduct product(String category, String code, Integer sortOrder, String servingMode) {
        CoffeeProduct p = new CoffeeProduct();
        p.setName(code);
        p.setProductCode(code);
        p.setStatus("active");
        p.setCategory(category);
        p.setSortOrder(sortOrder);
        p.setServingMode(servingMode);
        p.setPrice(new BigDecimal("20.00"));
        return p;
    }
}
