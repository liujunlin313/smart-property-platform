package com.smartproperty.platform.notification.vo;

import java.time.OffsetDateTime;

/**
 * Single-row notification header returned by MyBatis. Recipient collection
 * assembly belongs to the service because it is loaded by a separate query.
 */
public record NotificationDetailBaseVO(
        Long notificationId,
        String title,
        String content,
        String category,
        String status,
        OffsetDateTime sentAt,
        String senderUsername
) { }
