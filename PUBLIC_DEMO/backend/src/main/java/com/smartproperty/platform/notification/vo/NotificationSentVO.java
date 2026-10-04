package com.smartproperty.platform.notification.vo;

import java.time.OffsetDateTime;

public record NotificationSentVO(Long notificationId, String title, String category, String status, OffsetDateTime sentAt,
                                 String senderUsername, Long toCount, Long ccCount, Long recipientCount) { }
