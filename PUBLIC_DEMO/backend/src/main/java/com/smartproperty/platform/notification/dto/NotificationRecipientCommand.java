package com.smartproperty.platform.notification.dto;

/** One persisted recipient; charge payload stays recipient-scoped rather than on the shared notification. */
public record NotificationRecipientCommand(Long personId, String recipientRole, String payloadJson) { }
