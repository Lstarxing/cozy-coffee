package com.cozy.order.service.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderItemDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.service.product.ProductAddonResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单/商品 DTO 转换器。
 * 从 OrderServiceImpl 抽出，消除 1709 行上帝类中的转换逻辑（Phase 4.4）。
 *
 * N+1 修复（Phase 4.5）：toItemDTO 从逐条 productMapper.selectById
 * 改为批量预加载 productMap，消除循环内单条查询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDtoConverter {

    private final ObjectMapper objectMapper;
    private final CoffeeProductMapper productMapper;
    private final ProductAddonResolver productAddonResolver;

    /**
     * CoffeeProduct entity -> DTO
     */
    public CoffeeProductDTO toProductDTO(CoffeeProduct entity) {
        CoffeeProductDTO dto = new CoffeeProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setPriceMedium(entity.getPriceMedium());
        dto.setPriceLarge(entity.getPriceLarge());
        dto.setImageUrl(entity.getImageUrl());
        dto.setCategory(entity.getCategory());
        dto.setStatus(entity.getStatus());
        dto.setIsNewProduct(entity.getIsNewProduct());
        dto.setSizeType(entity.getSizeType());
        dto.setSugarType(entity.getSugarType());
        dto.setTempType(entity.getTempType());
        dto.setAddonGroups(productAddonResolver.loadMenuGroups(entity.getId()));
        return dto;
    }

    /**
     * ShopOrderItem entity -> DTO。
     * v4.5 N+1 修复：productMap 预加载，不再逐条 SELECT。
     */
    public ShopOrderItemDTO toItemDTO(ShopOrderItem entity, Map<Long, CoffeeProduct> productMap) {
        ShopOrderItemDTO dto = new ShopOrderItemDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setItemAmount(entity.getItemAmount());
        dto.setCupSize(entity.getCupSize());
        dto.setSugarLevel(entity.getSugarLevel());
        dto.setTemperature(entity.getTemperature());
        dto.setCoffeeStrength(entity.getCoffeeStrength());
        dto.setOptionsJson(entity.getOptionsJson());

        CoffeeProduct product = productMap != null ? productMap.get(entity.getProductId()) : null;
        if (product == null) {
            try {
                product = productMapper.selectById(entity.getProductId());
            } catch (Exception e) {
                log.warn("获取订单项商品图片失败: productId={}", entity.getProductId());
            }
        }
        if (product != null) {
            dto.setProductImage(product.getImageUrl());
        }
        return dto;
    }

    /**
     * 批量转换 items，预加载 productMap 避免 N+1。
     */
    public List<ShopOrderItemDTO> toItemDTOList(List<ShopOrderItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        List<Long> productIds = items.stream()
                .map(ShopOrderItem::getProductId).distinct().toList();
        Map<Long, CoffeeProduct> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(CoffeeProduct::getId, Function.identity()));
        return items.stream().map(item -> toItemDTO(item, productMap)).toList();
    }

    /**
     * Generic Object -> List<CoffeeProductDTO> for cache deserialization.
     */
    @SuppressWarnings("unchecked")
    public List<CoffeeProductDTO> convertToCoffeeProductList(Object cachedValue) {
        if (!(cachedValue instanceof List<?> rawList)) {
            return null;
        }
        return rawList.stream()
                .map(item -> objectMapper.convertValue(item, CoffeeProductDTO.class))
                .collect(Collectors.toList());
    }
}
