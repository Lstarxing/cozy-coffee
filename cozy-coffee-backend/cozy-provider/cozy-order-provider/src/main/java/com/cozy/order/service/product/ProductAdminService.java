package com.cozy.order.service.product;
import com.cozy.order.service.converter.OrderDtoConverter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.order.dto.request.AddonGroupRequest;
import com.cozy.order.dto.request.AddonItemRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ProductAddonDTO;
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
import com.cozy.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品管理服务（管理端）。
 * 从 OrderServiceImpl 抽出，承担商品 CRUD、上下架切换与加料组配置（P2-2）。
 *
 * 原 invalidateMenuCache 私有方法删除，直接复用 MenuCacheService.invalidate()。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private final CoffeeProductMapper productMapper;
    private final OrderDtoConverter dtoConverter;
    private final MenuCacheService menuCacheService;
    private final CoffeeProductAddonGroupMapper addonGroupMapper;
    private final CoffeeProductAddonMapper productAddonMapper;
    private final ProductAddonMapper addonMapper;
    private final CoffeeBeanMapper beanMapper;
    private final CoffeeBlendMapper blendMapper;
    private final ObjectMapper objectMapper;

    /** V2 咖啡系列分类：必须挂 bean_id / blend_id 二选一 */
    private static final Set<String> COFFEE_CATEGORIES = Set.of("ESPRESSO", "MILK", "SIGNATURE", "SPECIALTY");

    @Transactional
    public CoffeeProductDTO addProduct(CoffeeProductDTO dto) {
        if (dto == null) {
            throw new BusinessException("商品信息不能为空");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("商品价格不能为负数");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new BusinessException("商品分类不能为空");
        }

        CoffeeProduct product = new CoffeeProduct();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setPriceMedium(dto.getPriceMedium()); // v5.0
        product.setPriceLarge(dto.getPriceLarge()); // v5.0
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory().trim());
        product.setBeanId(dto.getBeanId());
        product.setBlendId(dto.getBlendId());
        validateBeanBlend(product.getCategory(), null, dto.getBeanId(), dto.getBlendId());
        product.setStatus("active");
        product.setSortOrder(0);
        product.setIsNewProduct(dto.getIsNewProduct() != null ? dto.getIsNewProduct() : false); // v5.0

        // v5.2: SKU 配置字段
        product.setSizeType(dto.getSizeType() != null ? dto.getSizeType() : "MEDIUM_LARGE");
        product.setSugarType(dto.getSugarType() != null ? dto.getSugarType() : "FREE_CHOICE");
        product.setTempType(dto.getTempType() != null ? dto.getTempType() : "HOT_COLD");
        product.setDefaultSugarLevel(dto.getDefaultSugarLevel()); // NO_SUGAR_ONLY 商品为 NULL
        product.setTags(writeTags(dto.getTags()));

        // 手动设置时间戳（修复 MetaObjectHandler 可能未扫码到的问题）
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        validateIntegrity(product);
        productMapper.insert(product);
        menuCacheService.invalidate();
        return dtoConverter.toProductDTO(product);
    }

    @Transactional
    public CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO dto) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new BusinessException("商品名称不能为空");
            }
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) {
            if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("商品价格不能为负数");
            }
            product.setPrice(dto.getPrice());
        }
        // v5.0: 中/大杯价格更新（始终设置，允许清除）
        product.setPriceMedium(dto.getPriceMedium());
        product.setPriceLarge(dto.getPriceLarge());
        if (dto.getIsNewProduct() != null)
            product.setIsNewProduct(dto.getIsNewProduct());

        // v5.2: SKU 配置字段
        if (dto.getSizeType() != null)
            product.setSizeType(dto.getSizeType());
        if (dto.getSugarType() != null)
            product.setSugarType(dto.getSugarType());
        if (dto.getTempType() != null)
            product.setTempType(dto.getTempType());
        product.setDefaultSugarLevel(dto.getDefaultSugarLevel()); // 表单始终下发；NO_SUGAR_ONLY 为 NULL
        product.setTags(writeTags(dto.getTags()));

        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getCategory() != null) {
            if (dto.getCategory().trim().isEmpty()) {
                throw new BusinessException("商品分类不能为空");
            }
            product.setCategory(dto.getCategory().trim());
        }

        // 豆/拼配挂接（允许清除为 null；按最终分类 + serving 校验）
        product.setBeanId(dto.getBeanId());
        product.setBlendId(dto.getBlendId());
        validateBeanBlend(product.getCategory(), product.getServingMode(), dto.getBeanId(), dto.getBlendId());

        // 手动更新时间戳
        product.setUpdatedAt(LocalDateTime.now());

        validateIntegrity(product);
        productMapper.updateById(product);
        menuCacheService.invalidate();
        return dtoConverter.toProductDTO(product);
    }

    /**
     * 豆/拼配挂接校验（2.8）：咖啡系列二选一、非咖啡/烘焙 NULL、体验商品 bean 必填、bean/blend 存在且 active。
     */
    private void validateBeanBlend(String category, String servingMode, Long beanId, Long blendId) {
        if (beanId != null && blendId != null) {
            throw new BusinessException("bean_id 与 blend_id 只允许二选一");
        }
        boolean coffee = COFFEE_CATEGORIES.contains(category);
        if (coffee) {
            if ("FIXED_COMBINATION".equals(servingMode)) {
                if (beanId == null) throw new BusinessException("体验商品必须挂单品豆（bean_id）");
            } else if (beanId == null && blendId == null) {
                throw new BusinessException("咖啡商品必须挂单品豆或拼配豆");
            }
        } else {
            if (beanId != null || blendId != null) {
                throw new BusinessException("非咖啡/烘焙商品不能挂豆/拼配");
            }
        }
        if (beanId != null) {
            CoffeeBean bean = beanMapper.selectById(beanId);
            if (bean == null) throw new BusinessException("单品豆不存在: " + beanId);
            if (!"active".equals(bean.getStatus())) throw new BusinessException("inactive 豆禁止挂接: " + bean.getCode());
        }
        if (blendId != null) {
            CoffeeBlend blend = blendMapper.selectById(blendId);
            if (blend == null) throw new BusinessException("拼配豆不存在: " + blendId);
            if (!"active".equals(blend.getStatus())) throw new BusinessException("inactive 拼配禁止挂接: " + blend.getCode());
        }
    }

    /**
     * 2.8 完整性校验（Admin 保存时）：
     * 价格互斥（size_type ↔ price/medium/large）、sugar_type ↔ default_sugar_level、serving_mode 双向。
     */
    private void validateIntegrity(CoffeeProduct product) {
        boolean mediumLarge = "MEDIUM_LARGE".equals(product.getSizeType());
        if (mediumLarge) {
            if (product.getPrice() != null) throw new BusinessException("MEDIUM_LARGE 商品 price 必须为 NULL");
            if (product.getPriceMedium() == null || product.getPriceLarge() == null) {
                throw new BusinessException("MEDIUM_LARGE 商品中/大杯价必填");
            }
        } else {
            if (product.getPrice() == null) throw new BusinessException("DEFAULT 商品基础价必填");
            if (product.getPriceMedium() != null || product.getPriceLarge() != null) {
                throw new BusinessException("DEFAULT 商品中/大杯价必须为 NULL");
            }
        }

        if ("NO_SUGAR_ONLY".equals(product.getSugarType())) {
            if (product.getDefaultSugarLevel() != null) {
                throw new BusinessException("NO_SUGAR_ONLY 商品 default_sugar_level 必须为 NULL");
            }
        } else if (product.getSugarType() != null) {
            if (product.getDefaultSugarLevel() == null) {
                throw new BusinessException("FREE_CHOICE/MIN_LESS_SWEET 商品 default_sugar_level 必填");
            }
        }

        if ("FIXED_COMBINATION".equals(product.getServingMode())) {
            if (product.getServingConfig() == null || product.getServingDesc() == null || product.getServingDesc().isBlank()) {
                throw new BusinessException("FIXED_COMBINATION 商品 serving_config/serving_desc 必填");
            }
            validateServingConfig(product.getServingConfig());
        } else if (product.getServingMode() != null) {
            if (product.getServingConfig() != null) {
                throw new BusinessException("非固定组合 serving_config 必须为 NULL");
            }
        }
    }

    private void validateServingConfig(String json) {
        try {
            List<Map<String, Object>> config = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (config == null || config.isEmpty()) throw new BusinessException("serving_config 不能为空");
            for (Map<String, Object> item : config) {
                String type = String.valueOf(item.get("type"));
                Object qty = item.get("quantity");
                if (!Set.of("ESPRESSO", "POUR_OVER", "MILK_COFFEE").contains(type)) {
                    throw new BusinessException("serving type 非法: " + type);
                }
                if (!(qty instanceof Number) || ((Number) qty).intValue() <= 0) {
                    throw new BusinessException("serving quantity 必须 > 0");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("serving_config 格式非法");
        }
    }

    /** 2.8：奶咖必须有 MILK 组；黑咖/手冲/冷萃不得有 MILK 组 */
    private void validateMilkGroupRule(String category, List<AddonGroupRequest> groups) {
        boolean hasMilkGroup = groups != null && groups.stream().anyMatch(g -> "MILK".equals(g.getCategory()));
        if ("MILK".equals(category) && !hasMilkGroup) {
            throw new BusinessException("奶咖必须有 MILK 组");
        }
        if (("ESPRESSO".equals(category) || "SPECIALTY".equals(category)) && hasMilkGroup) {
            throw new BusinessException("黑咖/手冲/冷萃不得有 MILK 组");
        }
    }

    private String writeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            throw new BusinessException("标签序列化失败");
        }
    }

    @Transactional
    public void deleteProduct(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            log.info("删除商品幂等返回: productId={} 不存在", productId);
            return;
        }
        productMapper.deleteById(productId);
        menuCacheService.invalidate();
    }

    @Transactional
    public CoffeeProductDTO toggleProductStatus(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "inactive" : "active");
        productMapper.updateById(product);
        return dtoConverter.toProductDTO(product);
    }

    // ==================== V2 加料组配置（P2-2）====================

    /** 加料主数据目录（active），Admin 绑定组内项时选择 */
    public List<ProductAddonDTO> listAddonCatalog() {
        return addonMapper.selectList(new LambdaQueryWrapper<ProductAddon>()
                        .eq(ProductAddon::getStatus, "active")
                        .orderByAsc(ProductAddon::getSortOrder))
                .stream().map(a -> {
                    ProductAddonDTO dto = new ProductAddonDTO();
                    dto.setId(a.getId());
                    dto.setCode(a.getCode());
                    dto.setName(a.getName());
                    dto.setPrice(a.getPrice());
                    dto.setCategory(a.getCategory());
                    return dto;
                }).collect(Collectors.toList());
    }

    /** 全量替换商品加料组 + 组内项（删除重建，事务内） */
    @Transactional
    public void saveAddonGroups(Long productId, List<AddonGroupRequest> groups) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        validateMilkGroupRule(product.getCategory(), groups);

        Map<Long, ProductAddon> addonById = new HashMap<>();
        if (groups != null && !groups.isEmpty()) {
            List<Long> addonIds = groups.stream()
                    .flatMap(g -> g.getItems() == null ? java.util.stream.Stream.empty() : g.getItems().stream())
                    .map(AddonItemRequest::getAddonId).distinct().collect(Collectors.toList());
            if (!addonIds.isEmpty()) {
                addonMapper.selectList(new LambdaQueryWrapper<ProductAddon>()
                                .in(ProductAddon::getId, addonIds))
                        .forEach(a -> addonById.put(a.getId(), a));
            }
            for (AddonGroupRequest group : groups) {
                validateGroup(group, addonById);
            }
        }

        // 删除既有组 + 项
        List<CoffeeProductAddonGroup> oldGroups = addonGroupMapper.selectList(
                new LambdaQueryWrapper<CoffeeProductAddonGroup>()
                        .eq(CoffeeProductAddonGroup::getProductId, productId));
        if (!oldGroups.isEmpty()) {
            List<Long> oldGroupIds = oldGroups.stream().map(CoffeeProductAddonGroup::getId).collect(Collectors.toList());
            productAddonMapper.delete(new LambdaQueryWrapper<CoffeeProductAddon>()
                    .in(CoffeeProductAddon::getGroupId, oldGroupIds));
            addonGroupMapper.delete(new LambdaQueryWrapper<CoffeeProductAddonGroup>()
                    .eq(CoffeeProductAddonGroup::getProductId, productId));
        }

        // 插入新组 + 项
        if (groups != null) {
            for (AddonGroupRequest group : groups) {
                CoffeeProductAddonGroup g = new CoffeeProductAddonGroup();
                g.setProductId(productId);
                g.setCategory(group.getCategory());
                g.setSelectionMode(group.getSelectionMode());
                g.setMinSelect(group.getMinSelect() != null ? group.getMinSelect() : 0);
                g.setMaxSelect(group.getMaxSelect() != null ? group.getMaxSelect() : 1);
                g.setSortOrder(group.getSortOrder() != null ? group.getSortOrder() : 0);
                addonGroupMapper.insert(g);
                if (group.getItems() != null) {
                    for (AddonItemRequest item : group.getItems()) {
                        CoffeeProductAddon b = new CoffeeProductAddon();
                        b.setGroupId(g.getId());
                        b.setAddonId(item.getAddonId());
                        b.setIsDefault(Boolean.TRUE.equals(item.getIsDefault()));
                        b.setPriceDelta(item.getPriceDelta() != null ? item.getPriceDelta() : BigDecimal.ZERO);
                        b.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
                        productAddonMapper.insert(b);
                    }
                }
            }
        }

        menuCacheService.invalidate();
    }

    private void validateGroup(AddonGroupRequest group, Map<Long, ProductAddon> addonById) {
        if (group.getCategory() == null || group.getCategory().isBlank()) {
            throw new BusinessException("加料组类别不能为空");
        }
        if (!"SINGLE".equals(group.getSelectionMode()) && !"MULTI".equals(group.getSelectionMode())) {
            throw new BusinessException("选择模式非法: " + group.getSelectionMode());
        }
        int min = group.getMinSelect() != null ? group.getMinSelect() : 0;
        int max = group.getMaxSelect() != null ? group.getMaxSelect() : 1;
        if (min > max) {
            throw new BusinessException("min_select 不能大于 max_select");
        }
        int itemCount = group.getItems() == null ? 0 : group.getItems().size();
        if (max > itemCount) {
            throw new BusinessException("max_select 超过组内项数");
        }
        if (group.getItems() != null) {
            long defaultCount = group.getItems().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsDefault())).count();
            if ("SINGLE".equals(group.getSelectionMode()) && min > 0 && defaultCount != 1) {
                throw new BusinessException("SINGLE 必选组默认项数量必须为 1");
            }
            for (AddonItemRequest item : group.getItems()) {
                ProductAddon addon = addonById.get(item.getAddonId());
                if (addon == null) {
                    throw new BusinessException("加料不存在: " + item.getAddonId());
                }
                if (!"active".equals(addon.getStatus())) {
                    throw new BusinessException("inactive 加料禁止绑定: " + addon.getCode());
                }
                if (!group.getCategory().equals(addon.getCategory())) {
                    throw new BusinessException("加料类别与组不匹配: " + group.getCategory() + " vs " + addon.getCategory() + " (" + addon.getCode() + ")");
                }
                if (item.getPriceDelta() == null || item.getPriceDelta().signum() < 0) {
                    throw new BusinessException("price_delta 禁止负值");
                }
                if (Boolean.TRUE.equals(item.getIsDefault()) && item.getPriceDelta().signum() != 0) {
                    throw new BusinessException("默认项 price_delta 必须为 0");
                }
            }
        }
    }
}
