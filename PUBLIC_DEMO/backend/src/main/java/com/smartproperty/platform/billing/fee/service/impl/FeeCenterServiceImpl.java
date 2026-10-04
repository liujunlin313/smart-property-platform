package com.smartproperty.platform.billing.fee.service.impl;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.billing.fee.mapper.FeeCenterMapper;
import com.smartproperty.platform.billing.fee.query.*;
import com.smartproperty.platform.billing.fee.service.FeeCenterService;
import com.smartproperty.platform.billing.fee.vo.*;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
public class FeeCenterServiceImpl implements FeeCenterService {
    private final FeeCenterMapper mapper; private final AuthorizationService authorizationService; private final DataScopeService dataScopeService;
    public FeeCenterServiceImpl(FeeCenterMapper mapper, AuthorizationService authorizationService, DataScopeService dataScopeService) { this.mapper = mapper; this.authorizationService = authorizationService; this.dataScopeService = dataScopeService; }
    @Override public java.util.List<FeeTypeOptionVO> listActiveFeeTypes() { authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); return mapper.selectActiveFeeTypes(); }
    @Override public PageResult<FeeStandardVO> pageFeeStandards(FeeStandardQuery q) { authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); normalize(q); dataScopeService.apply(q); return PageResult.of(mapper.selectFeeStandardPage(q, offset(q.getPage(), q.getPageSize()), q.getPageSize()), mapper.countFeeStandards(q), q.getPage(), q.getPageSize()); }
    @Override public PageResult<PaymentListVO> pagePayments(PaymentQuery q) { authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); normalize(q); dataScopeService.apply(q); return PageResult.of(mapper.selectPaymentPage(q, offset(q.getPage(), q.getPageSize()), q.getPageSize()), mapper.countPayments(q), q.getPage(), q.getPageSize()); }
    @Override public OverdueOverviewVO getOverdueOverview(OverdueQuery q) { authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); normalize(q); dataScopeService.apply(q); return new OverdueOverviewVO(orZero(mapper.selectOutstandingAmount(q)), mapper.countOverdueProperties(q), mapper.countOverdueBills(q), mapper.selectOverdueTopProperties(q, 10)); }
    @Override public PageResult<OverdueBillVO> pageOverdueBills(OverdueQuery q) { authorizationService.requirePermission(PermissionCodes.BILLING_VIEW); normalize(q); dataScopeService.apply(q); return PageResult.of(mapper.selectOverduePage(q, offset(q.getPage(), q.getPageSize()), q.getPageSize()), mapper.countOverduePage(q), q.getPage(), q.getPageSize()); }
    private long offset(Integer page, Integer size) { return (long) (page - 1) * size; }
    private BigDecimal orZero(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    private void normalize(FeeStandardQuery q) { normalizePage(q.getPage(), q.getPageSize(), q::setPage, q::setPageSize); }
    private void normalize(PaymentQuery q) { normalizePage(q.getPage(), q.getPageSize(), q::setPage, q::setPageSize); validateMonth(q.getMonth()); }
    private void normalize(OverdueQuery q) { normalizePage(q.getPage(), q.getPageSize(), q::setPage, q::setPageSize); validateMonth(q.getMonth()); }
    private void validateMonth(Integer month) { if (month != null && (month < 1 || month > 12)) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_MONTH", "月份必须在 1 到 12 之间"); }
    private void normalizePage(Integer page, Integer size, java.util.function.Consumer<Integer> setPage, java.util.function.Consumer<Integer> setSize) { if (page == null || page < 1) setPage.accept(1); if (size == null || size < 1) setSize.accept(20); else if (size > 100) setSize.accept(100); }
}
