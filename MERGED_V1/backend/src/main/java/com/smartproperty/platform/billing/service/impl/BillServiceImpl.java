package com.smartproperty.platform.billing.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.billing.mapper.BillMapper;
import com.smartproperty.platform.billing.query.BillQuery;
import com.smartproperty.platform.billing.service.BillService;
import com.smartproperty.platform.billing.vo.*;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BillServiceImpl implements BillService {
    private final BillMapper billMapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;
    public BillServiceImpl(BillMapper billMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.billMapper = billMapper;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }
    @Override public PageResult<BillListVO> pageBills(BillQuery query) {
        authorizationService.requirePermission(PermissionCodes.BILLING_VIEW);
        normalize(query); dataScopeService.apply(query);
        long total = billMapper.countBills(query);
        return PageResult.of(billMapper.selectBillPage(query, (long) (query.getPage() - 1) * query.getPageSize(), query.getPageSize()), total, query.getPage(), query.getPageSize());
    }
    @Override public BillDetailVO getBillDetail(Long billId) {
        authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); dataScopeService.requireBill(billId); BillDetailBaseVO detail = billMapper.selectBillDetail(billId);
        if (detail == null) throw new BusinessException(HttpStatus.NOT_FOUND, "BILL_NOT_FOUND", "未找到指定账单");
        return new BillDetailVO(detail.billId(), detail.billNo(), detail.propertyNo(), detail.communityName(), detail.buildingName(), detail.unitName(), detail.billingYear(), detail.billingMonth(), detail.issuedAt(), detail.receivableAmount(), detail.receivedAmount(), detail.outstandingAmount(), detail.dueDate(), detail.status(), billMapper.selectBillItems(billId), billMapper.selectPayments(billId));
    }
    private void normalize(BillQuery q) {
        if (q.getPage() == null || q.getPage() < 1) q.setPage(1);
        if (q.getPageSize() == null || q.getPageSize() < 1) q.setPageSize(20);
        if (q.getPageSize() > 100) q.setPageSize(100);
        if (q.getMonth() != null && (q.getMonth() < 1 || q.getMonth() > 12)) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_MONTH", "月份必须在 1 到 12 之间"); if (q.getPropertyKeyword() != null) q.setPropertyKeyword(q.getPropertyKeyword().trim()); }
}
