package com.smartproperty.platform.notification.controller;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.notification.dto.NotificationHistoryQuery;
import com.smartproperty.platform.notification.dto.NotificationSendRequest;
import com.smartproperty.platform.notification.dto.NotificationTargetQuery;
import com.smartproperty.platform.notification.service.NotificationService;
import com.smartproperty.platform.notification.vo.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService=notificationService; }
    @PostMapping public NotificationDetailVO send(@Valid @RequestBody NotificationSendRequest request) { return notificationService.send(request); }
    @GetMapping public PageResult<NotificationSentVO> pageSent(NotificationHistoryQuery query) { return notificationService.pageSent(query); }
    @GetMapping("/{notificationId}") public NotificationDetailVO detail(@PathVariable Long notificationId) { return notificationService.getSentDetail(notificationId); }
    @GetMapping("/targets") public List<NotificationTargetOptionVO> targets(NotificationTargetQuery query) { return notificationService.searchTargets(query); }
}
