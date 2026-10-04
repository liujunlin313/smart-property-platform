package com.smartproperty.platform.rental.mapper;

import com.smartproperty.platform.rental.query.RentalQuery;
import com.smartproperty.platform.rental.vo.NamedCountVO;
import com.smartproperty.platform.rental.vo.AgencyOptionVO;
import com.smartproperty.platform.rental.vo.RentalDetailVO;
import com.smartproperty.platform.rental.vo.RentalListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RentalMapper {
    long countRentals(@Param("query") RentalQuery query);
    List<RentalListVO> selectRentalPage(@Param("query") RentalQuery query, @Param("offset") long offset, @Param("limit") int limit);
    RentalDetailVO selectRentalDetail(@Param("rentalListingId") Long rentalListingId);
    RentalSummaryRow selectSummary(@Param("year") Integer year);
    List<NamedCountVO> selectCommunityDistribution();
    List<NamedCountVO> selectModeDistribution();
    List<NamedCountVO> selectAgencyDistribution();
    List<NamedCountVO> selectStatusDistribution();
    List<NamedCountVO> selectRentRangeDistribution();
    List<NamedCountVO> selectLayoutDistribution();
    List<AgencyOptionVO> selectAgencyOptions();
    record RentalSummaryRow(long currentPublishedCount, long rentedHistoryCount, long selfCount, long agencyCount,
                            long rentedInPeriodCount, java.math.BigDecimal averageMonthlyRent, java.math.BigDecimal averageListingDays) { }
}
