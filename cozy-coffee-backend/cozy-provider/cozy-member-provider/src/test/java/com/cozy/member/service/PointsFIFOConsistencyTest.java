package com.cozy.member.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.entity.PointsLotConsumption;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.PointsLotConsumptionMapper;
import com.cozy.member.mapper.PointsLotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 积分 FIFO 扣减一致性测试
 * TC02: 跨批次扣减，先到期的批次优先扣减
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PointsFIFOConsistencyTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private PointsLotMapper pointsLotMapper;

    @Autowired
    private PointsLotConsumptionMapper consumptionMapper;

    private static final Long TEST_USER_ID = 99999L;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        memberInfoMapper.deleteById(TEST_USER_ID);
        // 清理该用户的所有批次和消费记录
        pointsLotMapper.selectList(null).stream()
                .filter(lot -> lot.getUserId().equals(TEST_USER_ID))
                .forEach(lot -> pointsLotMapper.deleteById(lot.getId()));
    }

    @Test
    @DisplayName("TC02: FIFO 跨批次扣减 - 先到期先扣")
    void testFIFODeductionAcrossBatches() {
        // =============================================
        // 数据准备
        // 批次A: 剩余100分，明天过期（应先扣）
        // 批次B: 剩余500分，后天过期（后扣）
        // =============================================

        // 1. 创建会员信息
        MemberInfo member = new MemberInfo();
        member.setUserId(TEST_USER_ID);
        member.setMemberLevel("basic");
        member.setCurrentPoints(600); // 总计600分
        member.setTotalPoints(600);
        member.setExpTotal(0);
        member.setConsecutiveSignDays(0);
        memberInfoMapper.insert(member);

        // 2. 创建批次A（明天过期，100分）
        PointsLot lotA = new PointsLot();
        lotA.setUserId(TEST_USER_ID);
        lotA.setInitialAmount(100);
        lotA.setRemaining(100);
        lotA.setSourceType("test");
        lotA.setExpiresAt(LocalDateTime.now().plusDays(1)); // 明天过期
        lotA.setCreatedAt(LocalDateTime.now().minusHours(2)); // 较早创建
        pointsLotMapper.insert(lotA);

        // 3. 创建批次B（后天过期，500分）
        PointsLot lotB = new PointsLot();
        lotB.setUserId(TEST_USER_ID);
        lotB.setInitialAmount(500);
        lotB.setRemaining(500);
        lotB.setSourceType("test");
        lotB.setExpiresAt(LocalDateTime.now().plusDays(2)); // 后天过期
        lotB.setCreatedAt(LocalDateTime.now().minusHours(1)); // 较晚创建
        pointsLotMapper.insert(lotB);

        Long lotAId = lotA.getId();
        Long lotBId = lotB.getId();

        // =============================================
        // 执行扣减：扣300分
        // 预期：批次A扣光100分，批次B扣200分
        // =============================================
        boolean result = memberService.consumePointsFIFO(TEST_USER_ID, 300, "test_deduct", null);
        assertTrue(result, "扣减应成功");

        // =============================================
        // 验证结果
        // =============================================

        // 1. 验证主表余额
        MemberInfo updatedMember = memberInfoMapper.selectByUserIdForUpdate(TEST_USER_ID);
        assertEquals(300, updatedMember.getCurrentPoints(), "主表余额应为300");

        // 2. 验证批次剩余
        PointsLot updatedLotA = pointsLotMapper.selectById(lotAId);
        PointsLot updatedLotB = pointsLotMapper.selectById(lotBId);

        assertEquals(0, updatedLotA.getRemaining(), "批次A应被扣光，剩余0");
        assertEquals(300, updatedLotB.getRemaining(), "批次B应剩余300（扣了200）");

        // 3. 验证扣减明细顺序（批次A的记录应先于批次B）
        List<PointsLotConsumption> consumptions = consumptionMapper.selectList(null);
        List<PointsLotConsumption> userConsumptions = consumptions.stream()
                .filter(c -> c.getUserId().equals(TEST_USER_ID))
                .toList();

        assertEquals(2, userConsumptions.size(), "应有2条扣减明细");

        // 第一条应是批次A
        assertEquals(lotAId, userConsumptions.get(0).getLotId(), "第一条扣减应来自批次A（先到期）");
        assertEquals(100, userConsumptions.get(0).getConsumeAmount(), "批次A应扣100分");

        // 第二条应是批次B
        assertEquals(lotBId, userConsumptions.get(1).getLotId(), "第二条扣减应来自批次B");
        assertEquals(200, userConsumptions.get(1).getConsumeAmount(), "批次B应扣200分");
    }

    @Test
    @DisplayName("TC02-2: FIFO 单批次扣减 - 只扣第一个到期批次")
    void testFIFODeductionSingleBatch() {
        // 数据准备：同上
        MemberInfo member = new MemberInfo();
        member.setUserId(TEST_USER_ID);
        member.setMemberLevel("basic");
        member.setCurrentPoints(600);
        member.setTotalPoints(600);
        member.setExpTotal(0);
        member.setConsecutiveSignDays(0);
        memberInfoMapper.insert(member);

        PointsLot lotA = new PointsLot();
        lotA.setUserId(TEST_USER_ID);
        lotA.setInitialAmount(100);
        lotA.setRemaining(100);
        lotA.setSourceType("test");
        lotA.setExpiresAt(LocalDateTime.now().plusDays(1));
        lotA.setCreatedAt(LocalDateTime.now());
        pointsLotMapper.insert(lotA);

        PointsLot lotB = new PointsLot();
        lotB.setUserId(TEST_USER_ID);
        lotB.setInitialAmount(500);
        lotB.setRemaining(500);
        lotB.setSourceType("test");
        lotB.setExpiresAt(LocalDateTime.now().plusDays(2));
        lotB.setCreatedAt(LocalDateTime.now());
        pointsLotMapper.insert(lotB);

        Long lotAId = lotA.getId();
        Long lotBId = lotB.getId();

        // 执行扣减：只扣50分，应只扣批次A
        boolean result = memberService.consumePointsFIFO(TEST_USER_ID, 50, "test_deduct", null);
        assertTrue(result);

        // 验证
        PointsLot updatedLotA = pointsLotMapper.selectById(lotAId);
        PointsLot updatedLotB = pointsLotMapper.selectById(lotBId);

        assertEquals(50, updatedLotA.getRemaining(), "批次A应剩余50");
        assertEquals(500, updatedLotB.getRemaining(), "批次B应保持不变（500）");
    }
}
