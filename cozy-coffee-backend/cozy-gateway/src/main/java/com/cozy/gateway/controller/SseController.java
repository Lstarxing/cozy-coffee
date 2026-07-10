package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.SseService;
import com.cozy.gateway.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 管理端 SSE 控制器
 */
@RestController
@RequestMapping("/api/admin/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @PostMapping("/ticket")
    public Result<Map<String, String>> getTicket() {
        return Result.success(Map.of("ticket", sseService.generateTicket(AuthUtil.requireUserId())));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String ticket) {
        return sseService.connect(ticket);
    }

    @PostMapping("/disconnect")
    public Result<Void> disconnect() {
        sseService.disconnect(AuthUtil.requireUserId());
        return Result.success(null);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(Map.of("connections", sseService.getConnectionCount()));
    }
}
