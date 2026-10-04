package com.smartproperty.platform.statistics.service.impl;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.statistics.mapper.StatisticsMapper;
import com.smartproperty.platform.statistics.service.StatisticsService;
import com.smartproperty.platform.statistics.vo.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper statisticsMapper; private final AuthorizationService authorizationService; private final DataScopeService dataScopeService;
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) { this.statisticsMapper = statisticsMapper; this.authorizationService = authorizationService; this.dataScopeService = dataScopeService; }
    private CommunityDataScope scope(){authorizationService.requirePermission(PermissionCodes.BILLING_VIEW);return dataScopeService.currentCommunityScope();}
    public List<MonthlyCollectionVO> getMonthlyCollectionStatistics(Integer year) { return statisticsMapper.selectMonthlyCollection(year,scope()); }
    public AnnualCollectionVO getAnnualCollectionStatistics(Integer year) { var scope=scope(); AnnualCollectionSummaryVO annual = statisticsMapper.selectAnnualCollection(year,scope); if (annual == null) return new AnnualCollectionVO(year, 0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, List.of(), List.of()); return new AnnualCollectionVO(year, annual.billCount(), annual.receivableAmount(), annual.receivedAmount(), annual.outstandingAmount(), annual.collectionRatePercent(), statisticsMapper.selectQuarterlyCollection(year,scope), statisticsMapper.selectFeeComposition(year,scope)); }
    public List<CommunityMonthlyVO> getMonthlyCommunities(Integer year, Integer month) { var scope=scope(); validateMonth(month); return statisticsMapper.selectMonthlyCommunities(year, month,scope); }
    public MonthlyDrilldownVO getMonthlyDrilldown(Integer year, Integer month, Long communityId) { var scope=scope(); validateMonth(month); if(communityId!=null)dataScopeService.requireCommunity(communityId); MonthlyCollectionVO row = statisticsMapper.selectMonthlyDetailSummary(year, month, communityId,scope); String communityName = communityId == null ? "全部小区" : statisticsMapper.selectCommunityName(communityId,scope); return new MonthlyDrilldownVO(year, month, communityId, communityName, row.billCount(), row.receivableAmount(), row.receivedAmount(), row.outstandingAmount(), row.collectionRatePercent(), statisticsMapper.selectMonthlyFeeComposition(year, month, communityId,scope), statisticsMapper.selectMonthlyBillStatusDistribution(year, month, communityId,scope)); }
    private void validateMonth(Integer month) { if (month == null || month < 1 || month > 12) throw new com.smartproperty.platform.common.exception.BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_MONTH", "月份必须在 1 到 12 之间"); }
}
