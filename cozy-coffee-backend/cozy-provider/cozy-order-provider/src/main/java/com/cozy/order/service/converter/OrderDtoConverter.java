package com.cozy.order.service.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.order.dto.response.BeanProfileDTO;
import com.cozy.order.dto.response.BlendCompositionItem;
import com.cozy.order.dto.response.BlendProfileDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderItemDTO;
import com.cozy.order.entity.CoffeeBean;
import com.cozy.order.entity.CoffeeBlend;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeBeanMapper;
import com.cozy.order.mapper.CoffeeBlendMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.service.product.ProductAddonResolver;
import com.cozy.order.service.product.ProductRuleValidator;
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
    private final ProductRuleValidator ruleValidator;
    private final CoffeeBeanMapper beanMapper;
    private final CoffeeBlendMapper blendMapper;

    /**
     * CoffeeProduct entity -> DTO
     */
    public CoffeeProductDTO toProductDTO(CoffeeProduct entity) {
        CoffeeProductDTO dto = new CoffeeProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setProductCode(entity.getProductCode());
        dto.setDescription(entity.getDescription());
        dto.setShortDescription(entity.getShortDescription());
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
        dto.setDefaultSugarLevel(entity.getDefaultSugarLevel());
        dto.setServingMode(entity.getServingMode());
        dto.setServingDesc(entity.getServingDesc());
        dto.setTags(parseTags(entity.getTags()));
        dto.setBrewMethod(entity.getBrewMethod());
        dto.setColdBrewPrice(entity.getColdBrewPrice());
        dto.setBeanId(entity.getBeanId());
        dto.setBlendId(entity.getBlendId());
        dto.setAddonGroups(productAddonResolver.loadMenuGroups(entity.getId()));
        // 规格允许选项（单一事实源：后端由枚举规范值计算，前端渲染用）
        ProductRuleValidator.AllowedOptionsDTO opts = ruleValidator.getAllowedOptions(entity);
        dto.setAllowedSizes(java.util.Arrays.asList(opts.getAllowedSizes()));
        dto.setAllowedSugars(java.util.Arrays.asList(opts.getAllowedSugars()));
        dto.setAllowedTemps(java.util.Arrays.asList(opts.getAllowedTemps()));
        if (entity.getBeanId() != null) {
            CoffeeBean bean = beanMapper.selectById(entity.getBeanId());
            if (bean != null) dto.setBeanProfile(toBeanProfile(bean));
        }
        if (entity.getBlendId() != null) {
            CoffeeBlend blend = blendMapper.selectById(entity.getBlendId());
            if (blend != null) dto.setBlendProfile(toBlendProfile(blend));
        }
        return dto;
    }

    private BeanProfileDTO toBeanProfile(CoffeeBean bean) {
        BeanProfileDTO p = new BeanProfileDTO();
        p.setId(bean.getId());
        p.setCode(bean.getCode());
        p.setName(bean.getName());
        p.setNameEn(bean.getNameEn());
        p.setRoast(bean.getRoast());
        p.setFlavorNotes(bean.getFlavorNotes());
        p.setBody(bean.getBody());
        p.setAcidity(bean.getAcidity());
        return p;
    }

    private BlendProfileDTO toBlendProfile(CoffeeBlend blend) {
        BlendProfileDTO p = new BlendProfileDTO();
        p.setId(blend.getId());
        p.setCode(blend.getCode());
        p.setName(blend.getName());
        p.setNameEn(blend.getNameEn());
        p.setRoast(blend.getRoast());
        p.setFlavorNotes(blend.getFlavorNotes());
        p.setBody(blend.getBody());
        p.setAcidity(blend.getAcidity());
        p.setComposition(parseComposition(blend.getCompositionJson()));
        return p;
    }

    private List<BlendCompositionItem> parseComposition(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<BlendCompositionItem>>() {});
        } catch (Exception e) {
            log.warn("composition_json 解析失败: {}", json, e);
            return List.of();
        }
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("tags 解析失败: {}", json, e);
            return List.of();
        }
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
        dto.setBrewMethod(entity.getBrewMethod());
        dto.setCoffeeStrength(entity.getCoffeeStrength());
        dto.setOptionsJson(entity.getOptionsJson());
        dto.setAddonsJson(entity.getAddonsJson());

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
