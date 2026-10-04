package com.smartproperty.platform.rental.controller;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.rental.query.RentalQuery;
import com.smartproperty.platform.rental.service.RentalService;
import com.smartproperty.platform.rental.vo.RentalDetailVO;
import com.smartproperty.platform.rental.vo.RentalListVO;
import com.smartproperty.platform.rental.vo.RentalSummaryVO;
import com.smartproperty.platform.rental.vo.AgencyOptionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rentals")
public class RentalController {
    private final RentalService rentalService;
    public RentalController(RentalService rentalService) { this.rentalService = rentalService; }
    @GetMapping public PageResult<RentalListVO> pageRentals(RentalQuery query) { return rentalService.pageRentals(query); }
    @GetMapping("/summary") public RentalSummaryVO getSummary(@RequestParam(defaultValue = "2026") Integer year) { return rentalService.getSummary(year); }
    @GetMapping("/agencies") public List<AgencyOptionVO> listAgencyOptions() { return rentalService.listAgencyOptions(); }
    @GetMapping("/{rentalListingId}") public RentalDetailVO getRentalDetail(@PathVariable Long rentalListingId) { return rentalService.getRentalDetail(rentalListingId); }
}
