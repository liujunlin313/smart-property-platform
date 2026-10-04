package com.smartproperty.platform.rental.service;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.rental.query.RentalQuery;
import com.smartproperty.platform.rental.vo.RentalDetailVO;
import com.smartproperty.platform.rental.vo.RentalListVO;
import com.smartproperty.platform.rental.vo.RentalSummaryVO;
import com.smartproperty.platform.rental.vo.AgencyOptionVO;
import java.util.List;

public interface RentalService {
    PageResult<RentalListVO> pageRentals(RentalQuery query);
    RentalDetailVO getRentalDetail(Long rentalListingId);
    RentalSummaryVO getSummary(Integer year);
    List<AgencyOptionVO> listAgencyOptions();
}
