package com.cozy.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLifecycleEventContractTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 20, 12, 34, 56);

    @Test
    void allFiveFactsCarryBusinessKeyAndOccurrenceTimeThroughJson() throws Exception {
        assertEnvelope(UserCreatedEvent.builder().userId(38L)
                .uniqueKey(UserEventKeys.userCreated(38L)).occurredAt(occurredAt).build(), UserCreatedEvent.class);
        assertEnvelope(WelcomeGiftEligibleEvent.builder().userId(38L)
                .uniqueKey(UserEventKeys.welcomeGift(38L)).occurredAt(occurredAt).build(),
                WelcomeGiftEligibleEvent.class);
        assertEnvelope(ProfileCompletedEvent.builder().userId(38L)
                .uniqueKey(UserEventKeys.profileCompleted(38L)).occurredAt(occurredAt).build(),
                ProfileCompletedEvent.class);

        BirthdaySetEvent birthday = roundTrip(BirthdaySetEvent.builder().userId(38L).benefitYear(2025)
                .uniqueKey(UserEventKeys.birthday(38L, 2025)).occurredAt(occurredAt).build(), BirthdaySetEvent.class);
        assertEquals(2025, birthday.getBenefitYear());

        InviteRewardEarnedEvent invite = roundTrip(InviteRewardEarnedEvent.builder()
                .inviteeUserId(38L).inviterId(7L).uniqueKey(UserEventKeys.inviteReward(38L, 7L))
                .occurredAt(occurredAt).build(), InviteRewardEarnedEvent.class);
        assertEquals(38L, invite.getInviteeUserId());
        assertEquals(7L, invite.getInviterId());
    }

    @Test
    void businessKeyFormatsRemainBackwardCompatible() {
        assertEquals("user_created_38", UserEventKeys.userCreated(38L));
        assertEquals("NEW_USER_COUPON_38", UserEventKeys.welcomeGift(38L));
        assertEquals("profile_completed_38", UserEventKeys.profileCompleted(38L));
        assertEquals("birthday_38_2025", UserEventKeys.birthday(38L, 2025));
        assertEquals("invite_firstorder_38_7", UserEventKeys.inviteReward(38L, 7L));
    }

    private <T extends UserLifecycleEvent> void assertEnvelope(T event, Class<T> type) throws Exception {
        T restored = roundTrip(event, type);
        assertEquals(event.getUniqueKey(), restored.getUniqueKey());
        assertEquals(occurredAt, restored.getOccurredAt());
    }

    private <T> T roundTrip(T event, Class<T> type) throws Exception {
        String json = mapper.writeValueAsString(event);
        assertTrue(json.contains("uniqueKey"));
        assertTrue(json.contains("occurredAt"));
        return mapper.readValue(json, type);
    }
}
