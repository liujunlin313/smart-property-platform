package com.smartproperty.platform.notification.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.notification.charge.ChargeNoticeComposer;
import com.smartproperty.platform.notification.dto.*;
import com.smartproperty.platform.notification.mapper.NotificationMapper;
import com.smartproperty.platform.notification.preview.NotificationPreviewLogger;
import com.smartproperty.platform.notification.service.NotificationService;
import com.smartproperty.platform.notification.vo.*;
import org.apache.ibatis.annotations.Options;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Set<String> CATEGORIES = Set.of("PROPERTY_NOTICE", "MAINTENANCE", "BILLING", "EMERGENCY", "OTHER");
    private final NotificationMapper mapper; private final AuthorizationService authorizationService; private final DataScopeService dataScopeService; private final ChargeNoticeComposer chargeNoticeComposer; private final ObjectProvider<NotificationPreviewLogger> notificationPreviewLogger;
    public NotificationServiceImpl(NotificationMapper mapper, AuthorizationService authorizationService, DataScopeService dataScopeService, ChargeNoticeComposer chargeNoticeComposer, ObjectProvider<NotificationPreviewLogger> notificationPreviewLogger) { this.mapper=mapper; this.authorizationService=authorizationService; this.dataScopeService=dataScopeService; this.chargeNoticeComposer=chargeNoticeComposer; this.notificationPreviewLogger=notificationPreviewLogger; }
    @Override @Transactional public NotificationDetailVO send(NotificationSendRequest request) {
        var actor=authorizationService.requirePermission(PermissionCodes.NOTIFICATION_SEND);
        String category=normalizeCategory(request.category());
        LinkedHashSet<Long> to=resolveTargets(request.toTargets());
        if(to.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST,"NOTIFICATION_RECIPIENT_EMPTY","主要接收对象没有解析到有效居民");
        LinkedHashSet<Long> cc=resolveTargets(request.ccTargets()); cc.removeAll(to);
        List<Long> allRecipients=new ArrayList<>(to); allRecipients.addAll(cc);
        Map<Long,String> chargePayloads="BILLING".equals(category)
                ? chargePayloads(allRecipients, request.feeTypeId()) : Map.of();
        NotificationCreateCommand command=new NotificationCreateCommand(actor.accountId(), request.title().trim(), request.content().trim(), category);
        mapper.insertNotification(command);
        List<NotificationRecipientCommand> recipients = recipientCommands(to, cc, chargePayloads);
        mapper.insertRecipients(command.getNotificationId(), recipients);
        previewAfterCommit(command.getNotificationId(), category, command.getContent(), recipients);
        return detailOwned(command.getNotificationId(), actor.accountId());
    }
    @Override public PageResult<NotificationSentVO> pageSent(NotificationHistoryQuery query) {
        var actor=authorizationService.requirePermission(PermissionCodes.NOTIFICATION_VIEW); normalizePage(query);
        long total=mapper.countSent(actor.accountId());
        return PageResult.of(mapper.selectSentPage(actor.accountId(), (long)(query.getPage()-1)*query.getPageSize(), query.getPageSize()), total, query.getPage(), query.getPageSize());
    }
    @Override public NotificationDetailVO getSentDetail(Long notificationId) {
        var actor=authorizationService.requirePermission(PermissionCodes.NOTIFICATION_VIEW); return detailOwned(notificationId, actor.accountId());
    }
    @Override public List<NotificationTargetOptionVO> searchTargets(NotificationTargetQuery query) {
        authorizationService.requirePermission(PermissionCodes.NOTIFICATION_SEND);
        normalizeTargetQuery(query);
        if(query.getParentType()!=null) verifyTargetScope(query.getParentType(), query.getParentId());
        return mapper.searchTargetOptions(query, dataScopeService.currentCommunityScope());
    }
    private NotificationDetailVO detailOwned(Long id, Long accountId) {
        NotificationDetailBaseVO detail=mapper.selectSentDetailBase(id, accountId);
        if(detail==null) throw new BusinessException(HttpStatus.NOT_FOUND,"NOTIFICATION_NOT_FOUND","未找到该已发送通知");
        return new NotificationDetailVO(detail.notificationId(),detail.title(),detail.content(),detail.category(),detail.status(),detail.sentAt(),detail.senderUsername(),mapper.selectRecipients(id));
    }
    private LinkedHashSet<Long> resolveTargets(List<NotificationTargetRequest> targets) {
        LinkedHashSet<Long> resolved=new LinkedHashSet<>(); if(targets==null) return resolved;
        for(NotificationTargetRequest target:targets) { String type=normalizeTargetType(target.targetType()); for(Long id:target.targetIds()) { verifyTargetScope(type,id); resolved.addAll(mapper.selectRecipientPersonIds(type,id)); } }
        return resolved;
    }
    private Map<Long,String> chargePayloads(List<Long> recipientIds, Long feeTypeId) {
        authorizationService.requirePermission(PermissionCodes.BILLING_VIEW);
        return chargeNoticeComposer.compose(recipientIds, feeTypeId, dataScopeService.currentCommunityScope());
    }
    private void previewAfterCommit(Long notificationId, String category, String content, List<NotificationRecipientCommand> recipients) {
        notificationPreviewLogger.ifAvailable(preview -> {
            Runnable action = () -> preview.preview(notificationId, category, content, recipients);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { action.run(); } });
            } else action.run();
        });
    }
    private List<NotificationRecipientCommand> recipientCommands(LinkedHashSet<Long> to, LinkedHashSet<Long> cc, Map<Long,String> payloads) {
        List<NotificationRecipientCommand> commands=new ArrayList<>();
        to.forEach(personId -> commands.add(new NotificationRecipientCommand(personId,"TO",payloads.get(personId))));
        cc.forEach(personId -> commands.add(new NotificationRecipientCommand(personId,"CC",payloads.get(personId))));
        return commands;
    }
    private void verifyTargetScope(String type, Long id) {
        if(id==null || id<1) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","通知目标不能为空");
        switch(type) { case "COMMUNITY" -> dataScopeService.requireCommunity(id); case "BUILDING" -> dataScopeService.requireBuilding(id); case "PROPERTY" -> dataScopeService.requireProperty(id); case "PERSON" -> dataScopeService.requireCurrentResidentPerson(id); default -> throw new IllegalStateException(); }
    }
    private String normalizeTargetType(String raw) { String value=raw==null?"":raw.trim().toUpperCase(Locale.ROOT); if(!Set.of("COMMUNITY","BUILDING","PROPERTY","PERSON").contains(value)) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","目标类型必须为小区、楼栋、房产或人员"); return value; }
    private void normalizeTargetQuery(NotificationTargetQuery query) {
        if(query==null) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","目标查询不能为空");
        query.setKeyword(query.getKeyword()==null?"":query.getKeyword().trim());
        query.setLimit(query.getLimit()==null?30:Math.min(Math.max(query.getLimit(),1),50));
        String parent=query.getParentType()==null||query.getParentType().isBlank()?null:normalizeTargetType(query.getParentType());
        query.setParentType(parent);
        if(parent==null && query.getParentId()!=null) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","父级类型不能为空");
        if(parent!=null && (query.getParentId()==null || query.getParentId()<1)) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","父级对象不能为空");
        String requested=query.getTargetType()==null||query.getTargetType().isBlank()?"ALL":query.getTargetType().trim().toUpperCase(Locale.ROOT);
        if(!"ALL".equals(requested)) requested=normalizeTargetType(requested);
        if(parent!=null) {
            String child=switch(parent) { case "COMMUNITY" -> "BUILDING"; case "BUILDING" -> "PROPERTY"; case "PROPERTY" -> "PERSON"; default -> throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","人员不能继续展开"); };
            if(!"ALL".equals(requested) && !child.equals(requested)) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_TARGET","当前层级不支持该对象类型");
            query.setTargetType(child);
        } else if(query.getKeyword().isBlank()) query.setTargetType("COMMUNITY");
        else query.setTargetType(requested);
    }
    private String normalizeCategory(String raw) { String value=raw==null?"":raw.trim().toUpperCase(Locale.ROOT); if(!CATEGORIES.contains(value)) throw new BusinessException(HttpStatus.BAD_REQUEST,"INVALID_NOTIFICATION_CATEGORY","通知类型不受支持"); return value; }
    private void normalizePage(NotificationHistoryQuery query) { if(query.getPage()==null||query.getPage()<1) query.setPage(1); if(query.getPageSize()==null||query.getPageSize()<1) query.setPageSize(20); if(query.getPageSize()>100) query.setPageSize(100); }
}
