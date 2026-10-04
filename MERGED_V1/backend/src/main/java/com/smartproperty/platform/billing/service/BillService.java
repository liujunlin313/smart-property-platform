package com.smartproperty.platform.billing.service;
import com.smartproperty.platform.billing.query.BillQuery;
import com.smartproperty.platform.billing.vo.*;
import com.smartproperty.platform.common.api.PageResult;
public interface BillService { PageResult<BillListVO> pageBills(BillQuery query); BillDetailVO getBillDetail(Long billId); }
