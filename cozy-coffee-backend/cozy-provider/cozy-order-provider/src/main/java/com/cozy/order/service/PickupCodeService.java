package com.cozy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.order.entity.PickupCodeCounter;
import com.cozy.order.mapper.PickupCodeCounterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 取餐码生成服务
 * 规则：3位数字(001-999)，按营业日递增，同店同日不重复
 * 营业日定义：05:00开始为新的一天
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupCodeService {

    private final PickupCodeCounterMapper counterMapper;

    // 营业日开始时间（05:00）
    private static final LocalTime BUSINESS_DAY_START = LocalTime.of(5, 0);

    // 默认门店ID
    private static final Long DEFAULT_STORE_ID = 1L;

    /**
     * 计算营业日期
     * 规则：05:00前属于前一天的营业日
     */
    public LocalDate calculateBusinessDate(LocalDateTime orderTime) {
        if (orderTime.toLocalTime().isBefore(BUSINESS_DAY_START)) {
            return orderTime.toLocalDate().minusDays(1);
        }
        return orderTime.toLocalDate();
    }

    /**
     * 生成取餐码（事务内）
     * 
     * @return 3位取餐码字符串，如 "001", "037", "999"
     */
    @Transactional
    public String generatePickupCode() {
        return generatePickupCode(DEFAULT_STORE_ID, LocalDateTime.now());
    }

    /**
     * 生成取餐码（指定门店和时间）
     */
    @Transactional
    public String generatePickupCode(Long storeId, LocalDateTime orderTime) {
        LocalDate businessDate = calculateBusinessDate(orderTime);

        // 1. 获取或创建计数器（加锁）
        PickupCodeCounter counter = counterMapper.selectForUpdate(storeId, businessDate);

        if (counter == null) {
            // 创建新计数器
            counter = new PickupCodeCounter();
            counter.setStoreId(storeId);
            counter.setBusinessDate(businessDate);
            counter.setLastSeq(0);
            counterMapper.insert(counter);
            // 重新查询获取锁
            counter = counterMapper.selectForUpdate(storeId, businessDate);
        }

        // 2. 递增序号
        int nextSeq = counter.getLastSeq() + 1;

        // 3. 检查是否超过999
        if (nextSeq > 999) {
            log.error("取餐码耗尽! storeId={}, businessDate={}", storeId, businessDate);
            throw new RuntimeException("PICKUP_CODE_EXHAUSTED: 当日取餐码已用尽，请联系管理员");
        }

        // 4. 更新计数器
        counterMapper.incrementSeq(storeId, businessDate);

        // 5. 格式化为3位字符串
        String pickupCode = String.format("%03d", nextSeq);

        log.info("生成取餐码: storeId={}, businessDate={}, code={}", storeId, businessDate, pickupCode);

        return pickupCode;
    }

    /**
     * 获取当前营业日期
     */
    public LocalDate getCurrentBusinessDate() {
        return calculateBusinessDate(LocalDateTime.now());
    }
}
