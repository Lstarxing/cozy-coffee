package com.cozy.order.service.product;

import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.enums.SizeType;
import com.cozy.order.enums.SugarType;
import com.cozy.order.enums.TempType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SKU 配置验证服务
 * 用于验证订单中的杯型/甜度/温度选择是否符合产品配置规则
 */
@Slf4j
@Service
public class ProductRuleValidator {

    /**
     * 验证订单选项是否符合产品配置
     *
     * @param product     产品实体
     * @param sizeChoice  用户选择的杯型 (medium/large/standard)
     * @param sugarChoice 用户选择的甜度 (full/half/less/none)
     * @param tempChoice  用户选择的温度 (iced/hot)
     * @return 验证结果消息，null 表示通过
     */
    public String validateSpecs(CoffeeProduct product, 
                                    String sizeChoice, 
                                    String sugarChoice, 
                                    String tempChoice) {
        if (product == null) {
            return "产品不存在";
        }

        // 验证杯型
        String sizeError = validateSize(product, sizeChoice);
        if (sizeError != null) {
            return sizeError;
        }

        // 验证甜度
        String sugarError = validateSugar(product, sugarChoice);
        if (sugarError != null) {
            return sugarError;
        }

        // 验证温度
        String tempError = validateTemp(product, tempChoice);
        if (tempError != null) {
            return tempError;
        }

        return null; // 验证通过
    }

    /**
     * 验证杯型选择
     * v5.3: 允许 null 值（烘焙类商品不需要杯型）
     */
    private String validateSize(CoffeeProduct product, String sizeChoice) {
        if (sizeChoice == null || sizeChoice.isEmpty()) {
            // Only DEFAULT (standard cup) products can skip cup size
            String sizeTypeStr = product.getSizeType();
            if (sizeTypeStr == null || "DEFAULT".equals(sizeTypeStr)) {
                return null;
            }
            return "请选择杯型";
        }
        
        String sizeTypeStr = product.getSizeType();
        if (sizeTypeStr == null || sizeTypeStr.isEmpty()) {
            // 未配置则默认允许中大杯
            sizeTypeStr = "MEDIUM_LARGE";
        }

        try {
            SizeType sizeType = SizeType.valueOf(sizeTypeStr);
            
            if (!sizeType.isAllowed(sizeChoice)) {
                return String.format("产品 [%s] 不支持该杯型选择。允许的选项：%s", 
                    product.getName(), 
                    sizeType.getDescription());
            }
        } catch (IllegalArgumentException e) {
            log.warn("产品 {} 的杯型配置无效: {}", product.getName(), sizeTypeStr);
            return "产品配置错误，请联系管理员";
        }

        return null;
    }

    /**
     * 验证甜度选择
     * v5.3: 允许 null 值（烘焙类商品不需要甜度）
     */
    private String validateSugar(CoffeeProduct product, String sugarChoice) {
        // v5.3: 如果未传递甜度（如烘焙类商品），跳过验证
        if (sugarChoice == null || sugarChoice.isEmpty()) {
            return null;
        }
        
        String sugarTypeStr = product.getSugarType();
        if (sugarTypeStr == null || sugarTypeStr.isEmpty()) {
            // 未配置则默认自由选择
            sugarTypeStr = "FREE_CHOICE";
        }

        try {
            SugarType sugarType = SugarType.valueOf(sugarTypeStr);
            
            if (!sugarType.isAllowed(sugarChoice)) {
                String hint = "";
                if (sugarType == SugarType.MIN_LESS_SWEET) {
                    hint = "（此产品含糖浆/酱料，不可完全去糖）";
                } else if (sugarType == SugarType.NO_SUGAR_ONLY) {
                    hint = "（精品咖啡建议品尝原味）";
                }
                
                return String.format("产品 [%s] 不支持该甜度选择 %s。允许的选项：%s", 
                    product.getName(), 
                    hint,
                    sugarType.getDescription());
            }
        } catch (IllegalArgumentException e) {
            log.warn("产品 {} 的甜度配置无效: {}", product.getName(), sugarTypeStr);
            return "产品配置错误，请联系管理员";
        }

        return null;
    }

    /**
     * 验证温度选择
     * v5.3: 允许 null 值（烘焙类商品不需要温度）
     */
    private String validateTemp(CoffeeProduct product, String tempChoice) {
        // v5.3: 如果未传递温度（如烘焙类商品），跳过验证
        if (tempChoice == null || tempChoice.isEmpty()) {
            return null;
        }
        
        String tempTypeStr = product.getTempType();
        if (tempTypeStr == null || tempTypeStr.isEmpty()) {
            // 未配置则默认热/冰
            tempTypeStr = "HOT_COLD";
        }

        try {
            TempType tempType = TempType.valueOf(tempTypeStr);

            if (!tempType.isAllowed(tempChoice)) {
                String hint = "";
                if (tempType == TempType.HOT_ONLY) {
                    hint = "（奶泡工艺需要热饮）";
                } else if (tempType == TempType.COLD_ONLY) {
                    hint = "（产品工艺限定冰饮）";
                }

                return String.format("产品 [%s] 不支持该温度选择 %s。允许的选项：%s", 
                    product.getName(), 
                    hint,
                    tempType.getDescription());
            }
        } catch (IllegalArgumentException e) {
            log.warn("产品 {} 的温度配置无效: {}", product.getName(), tempTypeStr);
            return "产品配置错误，请联系管理员";
        }

        return null;
    }

    /**
     * 获取产品的允许选项（用于前端显示）
     *
     * @param product 产品实体
     * @return 允许的选项配置
     */
    public AllowedOptionsDTO getAllowedOptions(CoffeeProduct product) {
        AllowedOptionsDTO options = new AllowedOptionsDTO();
        
        // 杯型选项
        String sizeTypeStr = product.getSizeType() != null ? product.getSizeType() : "MEDIUM_LARGE";
        try {
            SizeType sizeType = SizeType.valueOf(sizeTypeStr);
            options.setAllowedSizes(sizeType.getAllowedValues());
        } catch (IllegalArgumentException e) {
            options.setAllowedSizes(new String[]{"medium", "large"});
        }

        // 甜度选项
        String sugarTypeStr = product.getSugarType() != null ? product.getSugarType() : "FREE_CHOICE";
        try {
            SugarType sugarType = SugarType.valueOf(sugarTypeStr);
            options.setAllowedSugars(sugarType.getAllowedValues());
        } catch (IllegalArgumentException e) {
            options.setAllowedSugars(new String[]{"full", "half", "less", "none"});
        }

        // 温度选项
        String tempTypeStr = product.getTempType() != null ? product.getTempType() : "HOT_COLD";
        try {
            TempType tempType = TempType.valueOf(tempTypeStr);
            options.setAllowedTemps(tempType.getAllowedValues());
        } catch (IllegalArgumentException e) {
            options.setAllowedTemps(new String[]{"iced", "hot"});
        }

        return options;
    }

    /**
     * SKU 选项 DTO
     */
    public static class AllowedOptionsDTO {
        private String[] allowedSizes;
        private String[] allowedSugars;
        private String[] allowedTemps;

        public String[] getAllowedSizes() {
            return allowedSizes;
        }

        public void setAllowedSizes(String[] allowedSizes) {
            this.allowedSizes = allowedSizes;
        }

        public String[] getAllowedSugars() {
            return allowedSugars;
        }

        public void setAllowedSugars(String[] allowedSugars) {
            this.allowedSugars = allowedSugars;
        }

        public String[] getAllowedTemps() {
            return allowedTemps;
        }

        public void setAllowedTemps(String[] allowedTemps) {
            this.allowedTemps = allowedTemps;
        }
    }
}
