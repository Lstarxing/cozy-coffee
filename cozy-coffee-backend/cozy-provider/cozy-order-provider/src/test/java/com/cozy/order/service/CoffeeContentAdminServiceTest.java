package com.cozy.order.service;

import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.BlendCompositionItem;
import com.cozy.order.dto.response.CoffeeBeanDTO;
import com.cozy.order.dto.response.CoffeeBlendDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeBean;
import com.cozy.order.entity.CoffeeBlend;
import com.cozy.order.entity.CoffeeOrigin;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeBeanMapper;
import com.cozy.order.mapper.CoffeeBlendMapper;
import com.cozy.order.mapper.CoffeeOriginMapper;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.cozy.order.service.converter.OrderDtoConverter;
import com.cozy.order.service.product.CoffeeContentAdminService;
import com.cozy.order.service.product.MenuCacheService;
import com.cozy.order.service.product.ProductAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 内容档案管理（P2 收尾）校验矩阵：
 * 拼配 composition（Σ=100 / 重复 bean / inactive bean / 空）、停用保护（bean 被 active 商品或拼配引用、
 * blend 被 active 商品引用、origin 被豆引用）、商品 bean/blend 二选一。
 */
class CoffeeContentAdminServiceTest {

    // ── 数据构造 ──────────────────────────────────────────

    private static CoffeeBean bean(Long id, String code, String status) {
        CoffeeBean b = new CoffeeBean();
        b.setId(id);
        b.setCode(code);
        b.setName("豆" + id);
        b.setStatus(status);
        return b;
    }

    private static CoffeeContentAdminService contentService(
            CoffeeOriginMapper originMapper, CoffeeBeanMapper beanMapper,
            CoffeeBlendMapper blendMapper, CoffeeProductMapper productMapper) {
        return new CoffeeContentAdminService(originMapper, beanMapper, blendMapper,
                productMapper, mock(MenuCacheService.class), new ObjectMapper());
    }

    private static CoffeeBlendDTO blendDTO(Long id, BlendCompositionItem... items) {
        CoffeeBlendDTO dto = new CoffeeBlendDTO();
        dto.setId(id);
        dto.setCode("BLEND_" + id);
        dto.setName("拼配" + id);
        dto.setComposition(List.of(items));
        return dto;
    }

    private static BlendCompositionItem item(Long beanId, int ratio) {
        BlendCompositionItem i = new BlendCompositionItem();
        i.setBeanId(beanId);
        i.setRatio(ratio);
        return i;
    }

    // ── 拼配 composition 校验 ─────────────────────────────

    @Test
    void saveBlend_rejectsRatioSumNot100() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "active"));
        when(beanMapper.selectById(2L)).thenReturn(bean(2L, "B", "active"));
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                mock(CoffeeBlendMapper.class), mock(CoffeeProductMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.saveBlend(blendDTO(1L, item(1L, 60), item(2L, 50))));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void saveBlend_rejectsDuplicateBean() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "active"));
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                mock(CoffeeBlendMapper.class), mock(CoffeeProductMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.saveBlend(blendDTO(1L, item(1L, 60), item(1L, 40))));
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    void saveBlend_rejectsInactiveBean() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "inactive"));
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                mock(CoffeeBlendMapper.class), mock(CoffeeProductMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.saveBlend(blendDTO(1L, item(1L, 100))));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void saveBlend_rejectsEmptyComposition() {
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), mock(CoffeeBeanMapper.class),
                mock(CoffeeBlendMapper.class), mock(CoffeeProductMapper.class));
        CoffeeBlendDTO dto = blendDTO(1L);
        dto.setComposition(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.saveBlend(dto));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    void saveBlend_validWritesCompositionJson() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "active"));
        when(beanMapper.selectById(2L)).thenReturn(bean(2L, "B", "active"));
        CoffeeBlendMapper blendMapper = mock(CoffeeBlendMapper.class);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                blendMapper, mock(CoffeeProductMapper.class));
        svc.saveBlend(blendDTO(null, item(1L, 60), item(2L, 40)));
        ArgumentCaptor<CoffeeBlend> captor = ArgumentCaptor.forClass(CoffeeBlend.class);
        verify(blendMapper).insert(captor.capture());
        assertTrue(captor.getValue().getCompositionJson().contains("\"beanId\":1"));
        assertTrue(captor.getValue().getCompositionJson().contains("\"ratio\":40"));
    }

    // ── 停用保护 ──────────────────────────────────────────

    @Test
    void deleteBean_rejectsWhenReferencedByActiveProduct() {
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        when(productMapper.selectCount(any())).thenReturn(1L);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), mock(CoffeeBeanMapper.class),
                mock(CoffeeBlendMapper.class), productMapper);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteBean(1L));
        assertTrue(ex.getMessage().contains("active 商品"));
    }

    @Test
    void deleteBean_rejectsWhenReferencedByActiveBlend() {
        CoffeeBlend blend = new CoffeeBlend();
        blend.setId(10L);
        blend.setName("VELVET");
        blend.setStatus("active");
        blend.setCompositionJson("[{\"beanId\":1,\"ratio\":60},{\"beanId\":2,\"ratio\":40}]");
        CoffeeBlendMapper blendMapper = mock(CoffeeBlendMapper.class);
        when(blendMapper.selectList(any())).thenReturn(List.of(blend));
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        when(productMapper.selectCount(any())).thenReturn(0L);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), mock(CoffeeBeanMapper.class),
                blendMapper, productMapper);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteBean(1L));
        assertTrue(ex.getMessage().contains("active 拼配"));
    }

    @Test
    void deleteBlend_rejectsWhenReferencedByActiveProduct() {
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        when(productMapper.selectCount(any())).thenReturn(1L);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), mock(CoffeeBeanMapper.class),
                mock(CoffeeBlendMapper.class), productMapper);
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteBlend(1L));
        assertTrue(ex.getMessage().contains("active 商品"));
    }

    @Test
    void deleteOrigin_rejectsWhenReferencedByBean() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectCount(any())).thenReturn(1L);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                mock(CoffeeBlendMapper.class), mock(CoffeeProductMapper.class));
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteOrigin(1L));
        assertTrue(ex.getMessage().contains("豆档案"));
    }

    @Test
    void saveBean_inactiveRejectedWhenReferencedByActiveProduct() {
        CoffeeBean existing = bean(1L, "A", "active");
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(existing);
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        when(productMapper.selectCount(any())).thenReturn(1L);
        CoffeeContentAdminService svc = contentService(mock(CoffeeOriginMapper.class), beanMapper,
                mock(CoffeeBlendMapper.class), productMapper);
        CoffeeBeanDTO dto = new CoffeeBeanDTO();
        dto.setId(1L);
        dto.setCode("A");
        dto.setName("豆");
        dto.setStatus("inactive");
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.saveBean(dto));
        assertTrue(ex.getMessage().contains("active 商品"));
    }

    // ── 商品 bean/blend 二选一（ProductAdminService） ──────

    private static ProductAdminService productService(CoffeeBeanMapper beanMapper, CoffeeBlendMapper blendMapper) {
        return new ProductAdminService(mock(CoffeeProductMapper.class), mock(OrderDtoConverter.class),
                mock(MenuCacheService.class), mock(CoffeeProductAddonGroupMapper.class),
                mock(CoffeeProductAddonMapper.class), mock(ProductAddonMapper.class), beanMapper, blendMapper);
    }

    private static CoffeeProductDTO productDTO(String category, Long beanId, Long blendId) {
        CoffeeProductDTO dto = new CoffeeProductDTO();
        dto.setName("P");
        dto.setPrice(new BigDecimal("20"));
        dto.setCategory(category);
        dto.setBeanId(beanId);
        dto.setBlendId(blendId);
        return dto;
    }

    @Test
    void addProduct_coffeeWithoutBeanRejected() {
        ProductAdminService svc = productService(mock(CoffeeBeanMapper.class), mock(CoffeeBlendMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.addProduct(productDTO("MILK", null, null)));
        assertTrue(ex.getMessage().contains("必须挂单品豆或拼配豆"));
    }

    @Test
    void addProduct_rejectsBeanAndBlendTogether() {
        ProductAdminService svc = productService(mock(CoffeeBeanMapper.class), mock(CoffeeBlendMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.addProduct(productDTO("MILK", 1L, 2L)));
        assertTrue(ex.getMessage().contains("二选一"));
    }

    @Test
    void addProduct_bakeryRejectsBean() {
        ProductAdminService svc = productService(mock(CoffeeBeanMapper.class), mock(CoffeeBlendMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.addProduct(productDTO("BAKERY", 1L, null)));
        assertTrue(ex.getMessage().contains("非咖啡/烘焙"));
    }

    @Test
    void addProduct_coffeeWithInactiveBeanRejected() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "inactive"));
        ProductAdminService svc = productService(beanMapper, mock(CoffeeBlendMapper.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.addProduct(productDTO("SIGNATURE", 1L, null)));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void addProduct_coffeeWithActiveBeanSucceeds() {
        CoffeeBeanMapper beanMapper = mock(CoffeeBeanMapper.class);
        when(beanMapper.selectById(1L)).thenReturn(bean(1L, "A", "active"));
        OrderDtoConverter converter = mock(OrderDtoConverter.class);
        when(converter.toProductDTO(any())).thenAnswer(inv -> {
            CoffeeProduct p = inv.getArgument(0);
            CoffeeProductDTO out = new CoffeeProductDTO();
            out.setName(p.getName());
            return out;
        });
        ProductAdminService svc = new ProductAdminService(mock(CoffeeProductMapper.class), converter,
                mock(MenuCacheService.class), mock(CoffeeProductAddonGroupMapper.class),
                mock(CoffeeProductAddonMapper.class), mock(ProductAddonMapper.class), beanMapper, mock(CoffeeBlendMapper.class));
        assertEquals("P", svc.addProduct(productDTO("SPECIALTY", 1L, null)).getName());
    }
}
