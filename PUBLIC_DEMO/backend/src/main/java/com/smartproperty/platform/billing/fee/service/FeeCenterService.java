package com.smartproperty.platform.billing.fee.service;
import com.smartproperty.platform.billing.fee.query.*;
import com.smartproperty.platform.billing.fee.vo.*;
import com.smartproperty.platform.common.api.PageResult;
public interface FeeCenterService {
    java.util.List<FeeTypeOptionVO> listActiveFeeTypes();
    PageResult<FeeStandardVO> pageFeeStandards(FeeStandardQuery query);
    PageResult<PaymentListVO> pagePayments(PaymentQuery query);
    OverdueOverviewVO getOverdueOverview(OverdueQuery query);
    PageResult<OverdueBillVO> pageOverdueBills(OverdueQuery query);
}
