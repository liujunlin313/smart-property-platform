package com.smartproperty.platform.billing.mapper;

import com.smartproperty.platform.billing.query.BillQuery;
import com.smartproperty.platform.billing.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BillMapper {
    long countBills(@Param("query") BillQuery query);
    List<BillListVO> selectBillPage(@Param("query") BillQuery query, @Param("offset") long offset, @Param("limit") int limit);
    BillDetailBaseVO selectBillDetail(@Param("billId") Long billId);
    List<BillItemVO> selectBillItems(@Param("billId") Long billId);
    List<PaymentVO> selectPayments(@Param("billId") Long billId);
}
