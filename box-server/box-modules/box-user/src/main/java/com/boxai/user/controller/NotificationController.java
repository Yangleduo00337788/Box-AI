package com.boxai.user.controller;

import com.boxai.common.result.Result;
import com.boxai.user.api.NotificationVO;
import com.boxai.user.application.NotificationApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping
    public Result<List<NotificationVO>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(notificationApplicationService.list(limit));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount() {
        return Result.success(Map.of("count", notificationApplicationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationApplicationService.markRead(id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationApplicationService.markAllRead();
        return Result.success();
    }
}
