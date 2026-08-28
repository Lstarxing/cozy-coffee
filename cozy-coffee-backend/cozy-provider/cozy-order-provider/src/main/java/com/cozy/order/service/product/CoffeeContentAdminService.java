package com.cozy.order.service.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.BlendCompositionItem;
import com.cozy.order.dto.response.CoffeeBeanDTO;
import com.cozy.order.dto.response.CoffeeBlendDTO;
import com.cozy.order.dto.response.CoffeeOriginDTO;
import com.cozy.order.entity.CoffeeBean;
import com.cozy.order.entity.CoffeeBlend;
import com.cozy.order.entity.CoffeeOrigin;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeBeanMapper;
import com.cozy.order.mapper.CoffeeBlendMapper;
import com.cozy.order.mapper.CoffeeOriginMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内容档案管理（管理端）：coffee_origin / coffee_bean / coffee_blend 三表 CRUD。
 * 含拼配 composition_json 校验（Σratio=100、bean active）与停用保护（被 active 商品/拼配引用禁止删除或停用）。
 * 从 ProductAdminService 拆出，独立职责域（内容层）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoffeeContentAdminService {

    private final CoffeeOriginMapper originMapper;
    private final CoffeeBeanMapper beanMapper;
    private final CoffeeBlendMapper blendMapper;
    private final CoffeeProductMapper productMapper;
    private final MenuCacheService menuCacheService;
    private final ObjectMapper objectMapper;

    // ==================== Origin ====================

    public List<CoffeeOriginDTO> listOrigins() {
        return originMapper.selectList(new LambdaQueryWrapper<CoffeeOrigin>()
                        .orderByAsc(CoffeeOrigin::getSortOrder))
                .stream().map(this::toOriginDTO).collect(Collectors.toList());
    }

    @Transactional
    public CoffeeOriginDTO saveOrigin(CoffeeOriginDTO dto) {
        if (dto == null) throw new BusinessException("产区信息不能为空");
        if (dto.getCode() == null || dto.getCode().isBlank()) throw new BusinessException("产区代码不能为空");
        if (dto.getCountry() == null || dto.getCountry().isBlank()) throw new BusinessException("产区国家不能为空");

        CoffeeOrigin origin = dto.getId() != null ? originMapper.selectById(dto.getId()) : null;
        if (dto.getId() != null && origin == null) throw new BusinessException("产区不存在");
        if (origin == null) {
            origin = new CoffeeOrigin();
            origin.setCreatedAt(LocalDateTime.now());
        }
        origin.setCode(dto.getCode().trim());
        origin.setCountry(dto.getCountry().trim());
        origin.setCountryZh(dto.getCountryZh());
        origin.setRegion(dto.getRegion());
        origin.setTypicalCharacter(dto.getTypicalCharacter());
        origin.setDescription(dto.getDescription());
        origin.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        origin.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        origin.setUpdatedAt(LocalDateTime.now());

        if (origin.getId() == null) originMapper.insert(origin);
        else originMapper.updateById(origin);
        return toOriginDTO(origin);
    }

    @Transactional
    public void deleteOrigin(Long id) {
        if (id == null) throw new BusinessException("产区ID不能为空");
        Long beanRefs = beanMapper.selectCount(new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getOriginId, id));
        if (beanRefs > 0) throw new BusinessException("该产区下仍有豆档案，请先解除引用");
        originMapper.deleteById(id);
    }

    // ==================== Bean ====================

    public List<CoffeeBeanDTO> listBeans() {
        List<CoffeeBean> beans = beanMapper.selectList(new LambdaQueryWrapper<CoffeeBean>()
                .orderByAsc(CoffeeBean::getSortOrder));
        Map<Long, String> originNames = originMapper.selectList(null).stream()
                .collect(Collectors.toMap(CoffeeOrigin::getId, CoffeeOrigin::getCountryZh, (a, b) -> a));
        return beans.stream().map(b -> toBeanDTO(b, originNames.get(b.getOriginId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public CoffeeBeanDTO saveBean(CoffeeBeanDTO dto) {
        if (dto == null) throw new BusinessException("豆档案不能为空");
        if (dto.getCode() == null || dto.getCode().isBlank()) throw new BusinessException("豆代码不能为空");
        if (dto.getName() == null || dto.getName().isBlank()) throw new BusinessException("豆名不能为空");
        if (dto.getOriginId() != null && originMapper.selectById(dto.getOriginId()) == null) {
            throw new BusinessException("产区不存在: " + dto.getOriginId());
        }

        CoffeeBean bean = dto.getId() != null ? beanMapper.selectById(dto.getId()) : null;
        if (dto.getId() != null && bean == null) throw new BusinessException("豆档案不存在");
        boolean toInactive = "inactive".equals(dto.getStatus())
                && bean != null && !"inactive".equals(bean.getStatus());
        if (toInactive) ensureBeanNotReferenced(dto.getId());

        if (bean == null) {
            bean = new CoffeeBean();
            bean.setCreatedAt(LocalDateTime.now());
        }
        bean.setCode(dto.getCode().trim());
        bean.setName(dto.getName().trim());
        bean.setNameEn(dto.getNameEn());
        bean.setOriginId(dto.getOriginId());
        bean.setAltitude(dto.getAltitude());
        bean.setProcessing(dto.getProcessing());
        bean.setVariety(dto.getVariety());
        bean.setRoast(dto.getRoast());
        bean.setFlavorNotes(dto.getFlavorNotes());
        bean.setBody(dto.getBody());
        bean.setAcidity(dto.getAcidity());
        bean.setRole(dto.getRole());
        bean.setDescription(dto.getDescription());
        bean.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        bean.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        bean.setUpdatedAt(LocalDateTime.now());

        if (bean.getId() == null) beanMapper.insert(bean);
        else beanMapper.updateById(bean);
        return toBeanDTO(bean, originNameOf(bean.getOriginId()));
    }

    @Transactional
    public void deleteBean(Long id) {
        if (id == null) throw new BusinessException("豆档案ID不能为空");
        ensureBeanNotReferenced(id);
        beanMapper.deleteById(id);
    }

    /** 停用保护：被任一 active 商品挂 bean_id 或被任一 active 拼配 composition 引用 → 拒绝 */
    private void ensureBeanNotReferenced(Long beanId) {
        Long productRefs = productMapper.selectCount(new LambdaQueryWrapper<CoffeeProduct>()
                .eq(CoffeeProduct::getBeanId, beanId)
                .eq(CoffeeProduct::getStatus, "active"));
        if (productRefs > 0) throw new BusinessException("该豆档案被 active 商品引用，请先解除绑定");
        blendMapper.selectList(new LambdaQueryWrapper<CoffeeBlend>().eq(CoffeeBlend::getStatus, "active"))
                .forEach(blend -> {
                    boolean ref = parseComposition(blend.getCompositionJson()).stream()
                            .anyMatch(c -> c.getBeanId() != null && c.getBeanId().equals(beanId));
                    if (ref) throw new BusinessException("该豆档案被 active 拼配引用: " + blend.getName());
                });
    }

    // ==================== Blend ====================

    public List<CoffeeBlendDTO> listBlends() {
        return blendMapper.selectList(new LambdaQueryWrapper<CoffeeBlend>()
                        .orderByAsc(CoffeeBlend::getSortOrder))
                .stream().map(this::toBlendDTO).collect(Collectors.toList());
    }

    @Transactional
    public CoffeeBlendDTO saveBlend(CoffeeBlendDTO dto) {
        if (dto == null) throw new BusinessException("拼配豆不能为空");
        if (dto.getCode() == null || dto.getCode().isBlank()) throw new BusinessException("拼配代码不能为空");
        if (dto.getName() == null || dto.getName().isBlank()) throw new BusinessException("拼配名不能为空");
        validateComposition(dto.getComposition());

        CoffeeBlend blend = dto.getId() != null ? blendMapper.selectById(dto.getId()) : null;
        if (dto.getId() != null && blend == null) throw new BusinessException("拼配豆不存在");
        boolean toInactive = "inactive".equals(dto.getStatus())
                && blend != null && !"inactive".equals(blend.getStatus());
        if (toInactive) {
            Long productRefs = productMapper.selectCount(new LambdaQueryWrapper<CoffeeProduct>()
                    .eq(CoffeeProduct::getBlendId, dto.getId())
                    .eq(CoffeeProduct::getStatus, "active"));
            if (productRefs > 0) throw new BusinessException("该拼配被 active 商品引用，请先解除绑定");
        }

        if (blend == null) {
            blend = new CoffeeBlend();
            blend.setCreatedAt(LocalDateTime.now());
        }
        blend.setCode(dto.getCode().trim());
        blend.setName(dto.getName().trim());
        blend.setNameEn(dto.getNameEn());
        blend.setDescription(dto.getDescription());
        blend.setCompositionJson(writeComposition(dto.getComposition()));
        blend.setRoast(dto.getRoast());
        blend.setFlavorNotes(dto.getFlavorNotes());
        blend.setBody(dto.getBody());
        blend.setAcidity(dto.getAcidity());
        blend.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        blend.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        blend.setUpdatedAt(LocalDateTime.now());

        if (blend.getId() == null) blendMapper.insert(blend);
        else blendMapper.updateById(blend);
        return toBlendDTO(blend);
    }

    @Transactional
    public void deleteBlend(Long id) {
        if (id == null) throw new BusinessException("拼配ID不能为空");
        Long productRefs = productMapper.selectCount(new LambdaQueryWrapper<CoffeeProduct>()
                .eq(CoffeeProduct::getBlendId, id)
                .eq(CoffeeProduct::getStatus, "active"));
        if (productRefs > 0) throw new BusinessException("该拼配被 active 商品引用，请先解除绑定");
        blendMapper.deleteById(id);
    }

    /** 拼配合法性：非空、ratio>0、beanId 唯一、bean 存在且 active、Σratio=100 */
    private void validateComposition(List<BlendCompositionItem> composition) {
        if (composition == null || composition.isEmpty()) {
            throw new BusinessException("拼配比例不能为空");
        }
        Set<Long> seen = new HashSet<>();
        int sum = 0;
        for (BlendCompositionItem item : composition) {
            if (item.getBeanId() == null) throw new BusinessException("拼配项 bean 不能为空");
            if (item.getRatio() == null || item.getRatio() <= 0) throw new BusinessException("拼配比例必须大于 0");
            if (!seen.add(item.getBeanId())) throw new BusinessException("拼配 bean 重复: " + item.getBeanId());
            CoffeeBean bean = beanMapper.selectById(item.getBeanId());
            if (bean == null) throw new BusinessException("拼配 bean 不存在: " + item.getBeanId());
            if (!"active".equals(bean.getStatus())) throw new BusinessException("inactive bean 禁止拼配: " + bean.getCode());
            sum += item.getRatio();
        }
        if (sum != 100) throw new BusinessException("拼配比例合计必须为 100，当前 " + sum);
    }

    // ==================== 转换 ====================

    private CoffeeOriginDTO toOriginDTO(CoffeeOrigin o) {
        CoffeeOriginDTO dto = new CoffeeOriginDTO();
        dto.setId(o.getId());
        dto.setCode(o.getCode());
        dto.setCountry(o.getCountry());
        dto.setCountryZh(o.getCountryZh());
        dto.setRegion(o.getRegion());
        dto.setTypicalCharacter(o.getTypicalCharacter());
        dto.setDescription(o.getDescription());
        dto.setSortOrder(o.getSortOrder());
        dto.setStatus(o.getStatus());
        return dto;
    }

    private CoffeeBeanDTO toBeanDTO(CoffeeBean b, String originName) {
        CoffeeBeanDTO dto = new CoffeeBeanDTO();
        dto.setId(b.getId());
        dto.setCode(b.getCode());
        dto.setName(b.getName());
        dto.setNameEn(b.getNameEn());
        dto.setOriginId(b.getOriginId());
        dto.setOriginName(originName);
        dto.setAltitude(b.getAltitude());
        dto.setProcessing(b.getProcessing());
        dto.setVariety(b.getVariety());
        dto.setRoast(b.getRoast());
        dto.setFlavorNotes(b.getFlavorNotes());
        dto.setBody(b.getBody());
        dto.setAcidity(b.getAcidity());
        dto.setRole(b.getRole());
        dto.setDescription(b.getDescription());
        dto.setSortOrder(b.getSortOrder());
        dto.setStatus(b.getStatus());
        return dto;
    }

    private CoffeeBlendDTO toBlendDTO(CoffeeBlend b) {
        CoffeeBlendDTO dto = new CoffeeBlendDTO();
        dto.setId(b.getId());
        dto.setCode(b.getCode());
        dto.setName(b.getName());
        dto.setNameEn(b.getNameEn());
        dto.setDescription(b.getDescription());
        dto.setComposition(parseComposition(b.getCompositionJson()));
        dto.setRoast(b.getRoast());
        dto.setFlavorNotes(b.getFlavorNotes());
        dto.setBody(b.getBody());
        dto.setAcidity(b.getAcidity());
        dto.setSortOrder(b.getSortOrder());
        dto.setStatus(b.getStatus());
        return dto;
    }

    private String originNameOf(Long originId) {
        if (originId == null) return null;
        CoffeeOrigin o = originMapper.selectById(originId);
        return o != null ? o.getCountryZh() : null;
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

    private String writeComposition(List<BlendCompositionItem> composition) {
        if (composition == null) return null;
        try {
            return objectMapper.writeValueAsString(composition);
        } catch (Exception e) {
            throw new BusinessException("拼配比例序列化失败");
        }
    }
}
