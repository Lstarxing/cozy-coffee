package com.cozy.order.service;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeBean;
import com.cozy.order.entity.CoffeeBlend;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.CoffeeProductAddon;
import com.cozy.order.entity.CoffeeProductAddonGroup;
import com.cozy.order.entity.ProductAddon;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuCacheServiceTest {

    private MenuCacheService cacheService;
    private CoffeeProductMapper productMapper;
    private CoffeeProductAddonGroupMapper groupMapper;
    private CoffeeProductAddonMapper productAddonMapper;
    private ProductAddonMapper addonMapper;
    private CoffeeBeanMapper beanMapper;
    private CoffeeBlendMapper blendMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        productMapper = mock(CoffeeProductMapper.class);
        groupMapper = mock(CoffeeProductAddonGroupMapper.class);
        productAddonMapper = mock(CoffeeProductAddonMapper.class);
        addonMapper = mock(ProductAddonMapper.class);
        beanMapper = mock(CoffeeBeanMapper.class);
        blendMapper = mock(CoffeeBlendMapper.class);
        ProductAddonResolver addonResolver = new ProductAddonResolver(
                groupMapper, productAddonMapper, addonMapper, new ObjectMapper());
        OrderDtoConverter dtoConverter = new OrderDtoConverter(new ObjectMapper(), productMapper, addonResolver,
                new ProductRuleValidator(), beanMapper, blendMapper);

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
        when(groupMapper.selectList(any())).thenReturn(List.of());

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
    void concurrentColdMissesShareOneRebuildWithoutReturningEmptyMenus() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        when(productMapper.selectList(any())).thenAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(100);
            return List.of(product());
        });

        ExecutorService executor = Executors.newFixedThreadPool(32);
        try {
            List<CompletableFuture<List<CoffeeProductDTO>>> requests = IntStream.range(0, 200)
                    .mapToObj(ignored -> CompletableFuture.supplyAsync(() -> {
                        try {
                            start.await();
                            return cacheService.getMenu();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                    }, executor))
                    .toList();

            start.countDown();
            for (CompletableFuture<List<CoffeeProductDTO>> request : requests) {
                assertFalse(request.get(5, TimeUnit.SECONDS).isEmpty());
            }
        } finally {
            executor.shutdownNow();
        }

        verify(productMapper, times(1)).selectList(any());
    }

    @Test
    void batchesMenuRelationsWithoutPerProductQueries() {
        CoffeeProduct beanProduct = product("SPECIALTY", "bean-product", 1, null);
        beanProduct.setId(1L);
        beanProduct.setBeanId(11L);
        CoffeeProduct blendProduct = product("SPECIALTY", "blend-product", 2, null);
        blendProduct.setId(2L);
        blendProduct.setBlendId(22L);
        when(productMapper.selectList(any())).thenReturn(List.of(beanProduct, blendProduct));

        CoffeeProductAddonGroup group1 = addonGroup(101L, 1L);
        CoffeeProductAddonGroup group2 = addonGroup(102L, 2L);
        when(groupMapper.selectList(any())).thenReturn(List.of(group1, group2));

        CoffeeProductAddon binding1 = addonBinding(101L, 201L);
        CoffeeProductAddon binding2 = addonBinding(102L, 201L);
        when(productAddonMapper.selectList(any())).thenReturn(List.of(binding1, binding2));

        ProductAddon addon = new ProductAddon();
        addon.setId(201L);
        addon.setCode("OAT_MILK");
        addon.setName("燕麦奶");
        when(addonMapper.selectList(any())).thenReturn(List.of(addon));

        CoffeeBean bean = new CoffeeBean();
        bean.setId(11L);
        bean.setCode("BEAN_11");
        CoffeeBlend blend = new CoffeeBlend();
        blend.setId(22L);
        blend.setCode("BLEND_22");
        when(beanMapper.selectBatchIds(any())).thenReturn(List.of(bean));
        when(blendMapper.selectBatchIds(any())).thenReturn(List.of(blend));

        List<CoffeeProductDTO> menu = cacheService.getMenu();

        assertEquals(2, menu.size());
        assertEquals(1, menu.get(0).getAddonGroups().size());
        assertEquals(1, menu.get(1).getAddonGroups().size());
        assertNotNull(menu.get(0).getBeanProfile());
        assertNotNull(menu.get(1).getBlendProfile());
        verify(groupMapper, times(1)).selectList(any());
        verify(productAddonMapper, times(1)).selectList(any());
        verify(addonMapper, times(1)).selectList(any());
        verify(beanMapper, times(1)).selectBatchIds(any());
        verify(blendMapper, times(1)).selectBatchIds(any());
        verify(beanMapper, never()).selectById(any());
        verify(blendMapper, never()).selectById(any());
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

    private CoffeeProductAddonGroup addonGroup(Long id, Long productId) {
        CoffeeProductAddonGroup group = new CoffeeProductAddonGroup();
        group.setId(id);
        group.setProductId(productId);
        group.setCategory("MILK");
        group.setSelectionMode("SINGLE");
        group.setMinSelect(1);
        group.setMaxSelect(1);
        group.setSortOrder(1);
        return group;
    }

    private CoffeeProductAddon addonBinding(Long groupId, Long addonId) {
        CoffeeProductAddon binding = new CoffeeProductAddon();
        binding.setGroupId(groupId);
        binding.setAddonId(addonId);
        binding.setIsDefault(true);
        binding.setPriceDelta(BigDecimal.ZERO);
        binding.setSortOrder(1);
        return binding;
    }
}
