package com.cozy.member.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.PointsLotMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PointsConsistencyTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private PointsLotMapper pointsLotMapper;

    @Test
    @Transactional
    public void testAdminRepairConsistency() {
        Long userId = 999L;
        // 1. 初始化用户
        memberService.createMember(userId);

        // 2. 模拟脏数据：直接修改 current_points 绕过 lot
        MemberInfo info = memberInfoMapper.selectByUserIdForUpdate(userId);
        info.setCurrentPoints(500);
        memberInfoMapper.updateById(info);

        // 3. 校验不一致
        int sumLots = pointsLotMapper.selectAvailableLotsForUpdate(userId).stream()
                .mapToInt(PointsLot::getRemaining).sum();
        assertNotEquals(500, sumLots, "数据应当不一致");

        // 4. 执行修复
        memberService.fixPointsConsistency(userId);

        // 5. 校验修复后一致
        int sumLotsAfter = pointsLotMapper.selectAvailableLotsForUpdate(userId).stream()
                .mapToInt(PointsLot::getRemaining).sum();
        assertEquals(500, sumLotsAfter, "修复后 SUM(lots) 应等于 current_points");
    }

    @Test
    @Transactional
    public void testAdminAdjustPositive() {
        Long userId = 998L;
        memberService.createMember(userId);
        int initialPoints = memberService.getMemberByUserId(userId).getCurrentPoints(); // 100 bonus

        memberService.adminAdjustPoints(userId, 200, "测试加分");

        var member = memberService.getMemberByUserId(userId);
        assertEquals(initialPoints + 200, member.getCurrentPoints());

        int sumLots = pointsLotMapper.selectAvailableLotsForUpdate(userId).stream()
                .mapToInt(PointsLot::getRemaining).sum();
        assertEquals(member.getCurrentPoints(), sumLots, "加分后 Lot 必须同步更新");
    }

    @Test
    @Transactional
    public void testAdminAdjustNegative() {
        Long userId = 997L;
        memberService.createMember(userId);
        memberService.adminAdjustPoints(userId, 500, "充值"); // Total 600

        memberService.adminAdjustPoints(userId, -200, "调整扣分");

        var member = memberService.getMemberByUserId(userId);
        assertEquals(400, member.getCurrentPoints());

        int sumLots = pointsLotMapper.selectAvailableLotsForUpdate(userId).stream()
                .mapToInt(PointsLot::getRemaining).sum();
        assertEquals(400, sumLots, "扣分后 Lot 必须同步扣减");
    }
}
