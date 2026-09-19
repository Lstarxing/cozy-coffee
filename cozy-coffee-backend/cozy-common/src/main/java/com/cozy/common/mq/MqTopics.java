package com.cozy.common.mq;

/**
 * RocketMQ 主题常量
 */
public final class MqTopics {

    private MqTopics() {
    }

    public static final String ORDER_EVENTS = "cozy-order-events";

    /** 会员域事件（发券请求等），消费方为 mall-provider */
    public static final String MEMBER_EVENTS = "cozy-member-events";
}
