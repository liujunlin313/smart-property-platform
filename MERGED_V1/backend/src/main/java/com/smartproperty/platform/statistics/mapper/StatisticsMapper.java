package com.smartproperty.platform.statistics.mapper;
import com.smartproperty.platform.statistics.vo.*;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface StatisticsMapper {
    List<MonthlyCollectionVO> selectMonthlyCollection(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    AnnualCollectionSummaryVO selectAnnualCollection(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<QuarterlyCollectionVO> selectQuarterlyCollection(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<FeeCompositionVO> selectFeeComposition(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<BillStatusDistributionVO> selectBillStatusDistribution(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<OccupancyOverviewVO> selectOccupancyOverview(@Param("scope") CommunityDataScope scope);
    List<CommunityMonthlyVO> selectMonthlyCommunities(@Param("year") Integer year, @Param("month") Integer month, @Param("scope") CommunityDataScope scope);
    MonthlyCollectionVO selectMonthlyDetailSummary(@Param("year") Integer year, @Param("month") Integer month, @Param("communityId") Long communityId, @Param("scope") CommunityDataScope scope);
    String selectCommunityName(@Param("communityId") Long communityId, @Param("scope") CommunityDataScope scope);
    List<FeeCompositionVO> selectMonthlyFeeComposition(@Param("year") Integer year, @Param("month") Integer month, @Param("communityId") Long communityId, @Param("scope") CommunityDataScope scope);
    List<BillStatusDistributionVO> selectMonthlyBillStatusDistribution(@Param("year") Integer year, @Param("month") Integer month, @Param("communityId") Long communityId, @Param("scope") CommunityDataScope scope);
}
