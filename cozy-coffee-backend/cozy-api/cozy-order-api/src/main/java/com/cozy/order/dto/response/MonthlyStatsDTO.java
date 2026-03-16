package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;

@Data
public class MonthlyStatsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int orderCount;
    private int morningOrderCount; // 10:00 前的单
    private int deliveryOrderCount; // 外卖单
    private int newProductCount; // 新品购买数
}
