package com.cozy.member.service.impl;

import com.cozy.member.entity.MemberInfo;
import com.cozy.member.mapper.MemberInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员建档的数据库级幂等（ADR 0001 C3）：member_info 的 UNIQUE INDEX user_id 是最后一道防线，
 * 冲突时按成功吸收，而不是把 DuplicateKeyException 抛给上游。
 */
@ExtendWith(MockitoExtension.class)
class MemberCreateIdempotencyTest {

    @Mock private MemberInfoMapper memberInfoMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @InjectMocks private MemberServiceImpl memberService;

    /** 并发/重投撞上 user_id 唯一索引时，按幂等吸收，不抛异常。 */
    @Test
    void duplicateKey_isAbsorbedAsIdempotent() {
        when(memberInfoMapper.selectCount(any())).thenReturn(0L);
        when(memberInfoMapper.insert(any(MemberInfo.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '9' for key 'user_id'"));

        assertDoesNotThrow(() -> memberService.createMember(9L));
    }

    /** 已存在时直接返回，连 insert 都不该发生。 */
    @Test
    void alreadyExists_skipsInsertEntirely() {
        when(memberInfoMapper.selectCount(any())).thenReturn(1L);

        memberService.createMember(9L);

        verify(memberInfoMapper, never()).insert(any(MemberInfo.class));
    }

    /** 建档初始状态：basic、三项计数全 0（v5.3 起注册不再送分）。 */
    @Test
    void createdMember_startsWithZeroState() {
        when(memberInfoMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<MemberInfo> captor = ArgumentCaptor.forClass(MemberInfo.class);

        memberService.createMember(9L);

        verify(memberInfoMapper).insert(captor.capture());
        MemberInfo inserted = captor.getValue();
        assertEquals(9L, inserted.getUserId());
        assertEquals("basic", inserted.getMemberLevel());
        assertEquals(0, inserted.getCurrentPoints().intValue());
        assertEquals(0, inserted.getTotalPoints().intValue());
        assertEquals(0, inserted.getExpTotal().intValue());
        assertEquals(0, inserted.getConsecutiveSignDays().intValue());
    }
}
