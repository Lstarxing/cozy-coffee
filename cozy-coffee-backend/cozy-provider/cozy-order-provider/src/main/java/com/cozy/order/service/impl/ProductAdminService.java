package com.cozy.order.service.impl;

import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品管理服务（管理端）。
 * 从 OrderServiceImpl 抽出，承担商品 CRUD 与上下架切换。
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

    @Transactional
    public CoffeeProductDTO addProduct(CoffeeProductDTO dto) {
        if (dto == null) {
            throw new RuntimeException("商品信息不能为空");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("商品价格不能为负数");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new RuntimeException("商品分类不能为空");
        }

        CoffeeProduct product = new CoffeeProduct();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setPriceMedium(dto.getPriceMedium()); // v5.0
        product.setPriceLarge(dto.getPriceLarge()); // v5.0
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory().trim());
        product.setStatus("active");
        product.setSortOrder(0);
        product.setIsNewProduct(dto.getIsNewProduct() != null ? dto.getIsNewProduct() : false); // v5.0

        // v5.2: SKU 配置字段
        product.setSizeType(dto.getSizeType() != null ? dto.getSizeType() : "MEDIUM_LARGE");
        product.setSugarType(dto.getSugarType() != null ? dto.getSugarType() : "FREE_CHOICE");
        product.setTempType(dto.getTempType() != null ? dto.getTempType() : "ALL_OK");

        // 手动设置时间戳（修复 MetaObjectHandler 可能未扫码到的问题）
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        productMapper.insert(product);
        menuCacheService.invalidate();
        return dtoConverter.toProductDTO(product);
    }

    @Transactional
    public CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO dto) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new RuntimeException("商品名称不能为空");
            }
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) {
            if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("商品价格不能为负数");
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

        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getCategory() != null) {
            if (dto.getCategory().trim().isEmpty()) {
                throw new RuntimeException("商品分类不能为空");
            }
            product.setCategory(dto.getCategory().trim());
        }

        // 手动更新时间戳
        product.setUpdatedAt(LocalDateTime.now());

        productMapper.updateById(product);
        menuCacheService.invalidate();
        return dtoConverter.toProductDTO(product);
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
            throw new RuntimeException("商品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "inactive" : "active");
        productMapper.updateById(product);
        return dtoConverter.toProductDTO(product);
    }
}
