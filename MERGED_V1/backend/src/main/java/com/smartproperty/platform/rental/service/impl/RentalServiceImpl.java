package com.smartproperty.platform.rental.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.rental.mapper.RentalMapper;
import com.smartproperty.platform.rental.query.RentalQuery;
import com.smartproperty.platform.rental.service.RentalService;
import com.smartproperty.platform.rental.vo.RentalDetailVO;
import com.smartproperty.platform.rental.vo.RentalListVO;
import com.smartproperty.platform.rental.vo.RentalSummaryVO;
import com.smartproperty.platform.rental.vo.AgencyOptionVO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RentalServiceImpl implements RentalService {
    private final RentalMapper rentalMapper;
    private final AuthorizationService authorizationService;
    public RentalServiceImpl(RentalMapper rentalMapper, AuthorizationService authorizationService) { this.rentalMapper = rentalMapper; this.authorizationService = authorizationService; }
    @Override public PageResult<RentalListVO> pageRentals(RentalQuery query) {
        authorizationService.requireSystemAdmin(); normalize(query);
        long total = rentalMapper.countRentals(query);
        return PageResult.of(rentalMapper.selectRentalPage(query, (long) (query.getPage() - 1) * query.getPageSize(), query.getPageSize()), total, query.getPage(), query.getPageSize());
    }
    @Override public RentalDetailVO getRentalDetail(Long rentalListingId) {
        authorizationService.requireSystemAdmin(); RentalDetailVO detail = rentalMapper.selectRentalDetail(rentalListingId);
        if (detail == null) throw new BusinessException(HttpStatus.NOT_FOUND, "RENTAL_LISTING_NOT_FOUND", "未找到指定挂牌");
        return detail;
    }
    @Override public RentalSummaryVO getSummary(Integer year) {
        authorizationService.requireSystemAdmin(); var row = rentalMapper.selectSummary(year);
        return new RentalSummaryVO(row.currentPublishedCount(), row.rentedHistoryCount(), row.selfCount(), row.agencyCount(), row.rentedInPeriodCount(), row.averageMonthlyRent(), row.averageListingDays(), rentalMapper.selectCommunityDistribution(), rentalMapper.selectModeDistribution(), rentalMapper.selectAgencyDistribution(), rentalMapper.selectStatusDistribution(), rentalMapper.selectRentRangeDistribution(), rentalMapper.selectLayoutDistribution());
    }
    @Override public List<AgencyOptionVO> listAgencyOptions() { authorizationService.requireSystemAdmin(); return rentalMapper.selectAgencyOptions(); }
    private void normalize(RentalQuery q) {
        if (q.getPage() == null || q.getPage() < 1) q.setPage(1);
        if (q.getPageSize() == null || q.getPageSize() < 1) q.setPageSize(20);
        if (q.getPageSize() > 100) q.setPageSize(100);
        if (q.getKeyword() != null) q.setKeyword(q.getKeyword().trim());
        if (q.getRentMin() != null && q.getRentMin().signum() < 0 || q.getRentMax() != null && q.getRentMax().signum() < 0) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RENT_RANGE", "租金筛选不能小于零");
        if (q.getRentMin() != null && q.getRentMax() != null && q.getRentMin().compareTo(q.getRentMax()) > 0) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RENT_RANGE", "最低租金不能高于最高租金");
    }
}
