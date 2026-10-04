package com.smartproperty.platform.notification.service;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.notification.dto.NotificationHistoryQuery;
import com.smartproperty.platform.notification.dto.NotificationSendRequest;
import com.smartproperty.platform.notification.dto.NotificationTargetQuery;
import com.smartproperty.platform.notification.vo.*;
import java.util.List;

public interface NotificationService {
    NotificationDetailVO send(NotificationSendRequest request);
    PageResult<NotificationSentVO> pageSent(NotificationHistoryQuery query);
    NotificationDetailVO getSentDetail(Long notificationId);
    List<NotificationTargetOptionVO> searchTargets(NotificationTargetQuery query);
}
