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
                                    String tempChoice,
                                    String brewMethod) {
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

        // 验证出品方式（精品 Bean）
        String brewError = validateBrewMethod(product, brewMethod, tempChoice);
        if (brewError != null) {
            return brewError;
        }

        return null; // 验证通过
    }

    /**
     * 验证出品方式（精品 Bean 商品必选）：POUR_OVER / COLD_BREW；Cold Brew 固定冰饮。
     */
    private String validateBrewMethod(CoffeeProduct product, String brewMethod, String tempChoice) {
        if (product.getBrewMethod() == null) {
            if (brewMethod != null && !brewMethod.isEmpty()) {
                return "该商品不支持出品方式选择";
            }
            return null;
        }
        if (brewMethod == null || brewMethod.isEmpty()) {
            return "请选择出品方式（手冲 / 冷萃）";
        }
        if (!"POUR_OVER".equals(brewMethod) && !"COLD_BREW".equals(brewMethod)) {
            return "出品方式非法: " + brewMethod;
        }
        if ("COLD_BREW".equals(brewMethod) && tempChoice != null && !"COLD".equalsIgnoreCase(tempChoice)) {
            return "冷萃仅限冰饮";
        }
        return null;
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
     * 获取产品的允许选项（单一事实源，用于前端渲染）。
     * 返回规范展示值（canonicalValues），区别于校验别名；烘焙/甜品等食品无糖度/温度选择，杯型固定单份。
     */
    public AllowedOptionsDTO getAllowedOptions(CoffeeProduct product) {
        AllowedOptionsDTO options = new AllowedOptionsDTO();
        if (isFoodCategory(product.getCategory())) {
            options.setAllowedSizes(new String[]{"STANDARD"});
            options.setAllowedSugars(new String[0]);
            options.setAllowedTemps(new String[0]);
            return options;
        }
        options.setAllowedSizes(canonical(SizeType.class, product.getSizeType(), "MEDIUM_LARGE", SizeType::canonicalValues));
        options.setAllowedSugars(canonical(SugarType.class, product.getSugarType(), "FREE_CHOICE", SugarType::canonicalValues));
        options.setAllowedTemps(canonical(TempType.class, product.getTempType(), "HOT_COLD", TempType::canonicalValues));
        return options;
    }

    private static boolean isFoodCategory(String category) {
        if (category == null) {
            return false;
        }
        String c = category.toLowerCase();
        return "bakery".equals(c) || "dessert".equals(c) || "food".equals(c) || "addon".equals(c);
    }

    private static <E extends Enum<E>> String[] canonical(Class<E> enumClass, String type,
            String defaultType, java.util.function.Function<E, String[]> values) {
        String t = type == null || type.isBlank() ? defaultType : type;
        try {
            return values.apply(Enum.valueOf(enumClass, t.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return values.apply(Enum.valueOf(enumClass, defaultType));
        }
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
