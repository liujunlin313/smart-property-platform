package com.smartproperty.platform.notification.mapper;

import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.notification.dto.NotificationCreateCommand;
import com.smartproperty.platform.notification.dto.NotificationRecipientCommand;
import com.smartproperty.platform.notification.dto.NotificationHistoryQuery;
import com.smartproperty.platform.notification.dto.NotificationTargetQuery;
import com.smartproperty.platform.notification.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NotificationMapper {
    @Options(useGeneratedKeys = true, keyProperty = "notificationId")
    int insertNotification(NotificationCreateCommand command);
    int insertRecipients(@Param("notificationId") Long notificationId, @Param("recipients") List<NotificationRecipientCommand> recipients);
    List<Long> selectRecipientPersonIds(@Param("targetType") String targetType, @Param("targetId") Long targetId);
    long countSent(@Param("accountId") Long accountId);
    List<NotificationSentVO> selectSentPage(@Param("accountId") Long accountId, @Param("offset") long offset, @Param("limit") int limit);
    NotificationDetailBaseVO selectSentDetailBase(@Param("notificationId") Long notificationId, @Param("accountId") Long accountId);
    List<NotificationRecipientVO> selectRecipients(@Param("notificationId") Long notificationId);
    List<NotificationTargetOptionVO> searchTargetOptions(@Param("query") NotificationTargetQuery query, @Param("scope") CommunityDataScope scope);
}
