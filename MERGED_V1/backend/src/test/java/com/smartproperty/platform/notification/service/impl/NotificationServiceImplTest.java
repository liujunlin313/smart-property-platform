package com.smartproperty.platform.notification.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.auth.context.ActorContext;
import com.smartproperty.platform.notification.dto.NotificationSendRequest;
import com.smartproperty.platform.notification.dto.NotificationTargetRequest;
import com.smartproperty.platform.notification.dto.NotificationTargetQuery;
import com.smartproperty.platform.notification.dto.NotificationRecipientCommand;
import com.smartproperty.platform.notification.charge.ChargeNoticeComposer;
import com.smartproperty.platform.notification.mapper.NotificationMapper;
import com.smartproperty.platform.notification.preview.NotificationPreviewLogger;
import com.smartproperty.platform.notification.vo.NotificationDetailBaseVO;
import com.smartproperty.platform.notification.vo.NotificationRecipientVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    @Test
    void blankTargetQueryStartsAtOnlyTheCurrentScopeCommunities() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        DataScopeService dataScopeService = mock(DataScopeService.class);
        NotificationServiceImpl service = new NotificationServiceImpl(mapper, authorizationService, dataScopeService,
                mock(ChargeNoticeComposer.class), previewProvider());
        when(authorizationService.requirePermission(anyString()))
                .thenReturn(new ActorContext(7L, null, "staff", "ACCOUNT", Set.of("PROPERTY_STAFF"), null));
        CommunityDataScope scope = new CommunityDataScope(false, List.of(3L));
        when(dataScopeService.currentCommunityScope()).thenReturn(scope);

        service.searchTargets(new NotificationTargetQuery());

        ArgumentCaptor<NotificationTargetQuery> query = ArgumentCaptor.forClass(NotificationTargetQuery.class);
        verify(mapper).searchTargetOptions(query.capture(), eq(scope));
        assertEquals("COMMUNITY", query.getValue().getTargetType());
        assertEquals(30, query.getValue().getLimit());
    }

    @Test
    void sendKeepsToPriorityWhenCcResolvesTheSamePerson() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        DataScopeService dataScopeService = mock(DataScopeService.class);
        ChargeNoticeComposer chargeNoticeComposer = mock(ChargeNoticeComposer.class);
        NotificationServiceImpl service = new NotificationServiceImpl(mapper, authorizationService, dataScopeService,
                chargeNoticeComposer, previewProvider());

        when(authorizationService.requirePermission(anyString()))
                .thenReturn(new ActorContext(7L, null, "admin", "ACCOUNT", Set.of("SYSTEM_ADMIN"), null));
        when(mapper.selectRecipientPersonIds("BUILDING", 42L)).thenReturn(List.of(101L, 102L));
        when(mapper.selectRecipientPersonIds("PERSON", 101L)).thenReturn(List.of(101L));
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, com.smartproperty.platform.notification.dto.NotificationCreateCommand.class)
                    .setNotificationId(91L);
            return 1;
        });
        when(mapper.selectSentDetailBase(91L, 7L)).thenReturn(new NotificationDetailBaseVO(
                91L, "维修", "通知正文", "MAINTENANCE", "SENT", OffsetDateTime.now(), "admin"));
        when(mapper.selectRecipients(91L)).thenReturn(List.of(
                new NotificationRecipientVO(101L, "甲", "TO"),
                new NotificationRecipientVO(102L, "乙", "TO")));

        var response = service.send(new NotificationSendRequest(
                "维修", "通知正文", "MAINTENANCE", null,
                List.of(new NotificationTargetRequest("BUILDING", List.of(42L))),
                List.of(new NotificationTargetRequest("PERSON", List.of(101L)))
        ));

        ArgumentCaptor<List<NotificationRecipientCommand>> recipients = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertRecipients(eq(91L), recipients.capture());
        assertEquals(List.of(101L, 102L), recipients.getValue().stream().map(NotificationRecipientCommand::personId).toList());
        assertEquals(List.of("TO", "TO"), recipients.getValue().stream().map(NotificationRecipientCommand::recipientRole).toList());
        assertEquals(2, response.recipients().size());
        assertEquals("TO", response.recipients().getFirst().recipientRole());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<NotificationPreviewLogger> previewProvider() {
        return mock(ObjectProvider.class);
    }
}
