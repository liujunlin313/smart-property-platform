package com.smartproperty.platform.notification.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record NotificationDetailVO(Long notificationId, String title, String content, String category, String status,
                                   OffsetDateTime sentAt, String senderUsername, List<NotificationRecipientVO> recipients) { }
