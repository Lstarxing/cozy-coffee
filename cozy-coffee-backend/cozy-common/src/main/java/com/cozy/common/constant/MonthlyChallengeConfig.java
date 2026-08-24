package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 月度挑战任务配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.monthly-challenge，items 即白皮书「月度挑战任务」表（key/title/description/target/reward）；
 * 新加/调整挑战只改 yml，无需改码。
 * 供 MonthlyTaskServiceImpl.buildChallenges（返回前端 challenges 列表）与
 * checkAndGrantRewards/tryGrantChallenge（达标判定与奖励发放）共用。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.monthly-challenge")
public class MonthlyChallengeConfig {

    /** 挑战任务列表（顺序即前端展示顺序） */
    private List<ChallengeItem> items = defaultItems();

    /** 按 key 取挑战配置；不存在返回 null */
    public ChallengeItem getItem(String key) {
        if (key == null || items == null) {
            return null;
        }
        return items.stream()
                .filter(i -> key.equals(i.getKey()))
                .findFirst()
                .orElse(null);
    }

    @Data
    public static class ChallengeItem {
        /** 挑战 key：order/morning/delivery/newproduct */
        private String key;
        private String title;
        private String description;
        /** 达标次数阈值 */
        private int target;
        /** 完成奖励积分 */
        private int reward;
        /** 对应 MonthlyStatsDTO 的计数字段名（orderCount/morningOrderCount/deliveryOrderCount/newProductCount） */
        private String statsField;
    }

    private static List<ChallengeItem> defaultItems() {
        List<ChallengeItem> list = new ArrayList<>();
        list.add(item("order", "打卡达人", "当月完成 4 笔订单", 4, 40, "orderCount"));
        list.add(item("morning", "晨间唤醒", "当月完成 3 笔上午 10 点前订单", 3, 60, "morningOrderCount"));
        list.add(item("delivery", "外卖尝鲜", "当月完成 2 笔外卖", 2, 50, "deliveryOrderCount"));
        list.add(item("newproduct", "新品猎人", "当月购买 3 款新品", 3, 80, "newProductCount"));
        return list;
    }

    private static ChallengeItem item(String key, String title, String description, int target, int reward,
            String statsField) {
        ChallengeItem i = new ChallengeItem();
        i.setKey(key);
        i.setTitle(title);
        i.setDescription(description);
        i.setTarget(target);
        i.setReward(reward);
        i.setStatsField(statsField);
        return i;
    }
}
