package com.cozy.gateway.api;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.api.PointsMallService;
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.api.SigninService;
import com.cozy.order.api.OrderService;
import com.cozy.user.api.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dubbo 接口方法必须声明 {@code throws BusinessException}（放在网关是因为只有这里能看到全部 api 模块）。
 *
 * <p>原因：Dubbo ExceptionFilter 只对「签名里声明过」或「与接口同 jar」的异常原样回传，否则包成
 * {@code RuntimeException(StringUtils.toString(e))} —— 客户端拿到的是堆栈字符串，且 errorCode / retryable 全丢。
 * 接口在 cozy-*-api jar、BusinessException 在 cozy-common jar，不同 jar，所以只能靠签名声明。
 *
 * <p>未受检异常的 throws 没有编译期强制，新增方法漏写不会报错，故用本测试兜住。
 */
class DubboApiExceptionContractTest {

    private static final List<Class<?>> API_INTERFACES = List.of(
            UserService.class,
            AddressService.class,
            MemberService.class,
            MonthlyTaskService.class,
            SigninService.class,
            OrderService.class,
            PointsMallService.class);

    @Test
    void everyDubboApiMethodDeclaresBusinessException() {
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Class<?> api : API_INTERFACES) {
            for (Method method : api.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge() || method.isDefault()) {
                    continue;
                }
                checked++;
                if (!Arrays.asList(method.getExceptionTypes()).contains(BusinessException.class)) {
                    missing.add(api.getSimpleName() + "#" + method.getName());
                }
            }
        }
        assertTrue(checked > 0, "没扫到任何 Dubbo 接口方法，测试本身失效了");
        assertTrue(missing.isEmpty(), "以下 Dubbo 接口方法未声明 throws BusinessException：" + missing);
    }
}
