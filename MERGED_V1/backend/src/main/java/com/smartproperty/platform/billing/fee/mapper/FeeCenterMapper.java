package com.smartproperty.platform.billing.fee.mapper;
import com.smartproperty.platform.billing.fee.query.FeeStandardQuery;
import com.smartproperty.platform.billing.fee.query.OverdueQuery;
import com.smartproperty.platform.billing.fee.query.PaymentQuery;
import com.smartproperty.platform.billing.fee.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;
@Mapper
public interface FeeCenterMapper {
    List<FeeTypeOptionVO> selectActiveFeeTypes();
    long countFeeStandards(@Param("query") FeeStandardQuery query);
    List<FeeStandardVO> selectFeeStandardPage(@Param("query") FeeStandardQuery query, @Param("offset") long offset, @Param("limit") int limit);
    long countPayments(@Param("query") PaymentQuery query);
    List<PaymentListVO> selectPaymentPage(@Param("query") PaymentQuery query, @Param("offset") long offset, @Param("limit") int limit);
    BigDecimal selectOutstandingAmount(@Param("query") OverdueQuery query);
    long countOverdueProperties(@Param("query") OverdueQuery query);
    long countOverdueBills(@Param("query") OverdueQuery query);
    List<OverduePropertyVO> selectOverdueTopProperties(@Param("query") OverdueQuery query, @Param("limit") int limit);
    long countOverduePage(@Param("query") OverdueQuery query);
    List<OverdueBillVO> selectOverduePage(@Param("query") OverdueQuery query, @Param("offset") long offset, @Param("limit") int limit);
}
