package com.smartproperty.platform.map.mapper;

import com.smartproperty.platform.map.model.BuildingLocationCandidate;
import com.smartproperty.platform.map.model.CommunityCentroidCandidate;
import com.smartproperty.platform.map.model.LocatedBuildingCoordinate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MapLocationMapper {
    List<BuildingLocationCandidate> selectActiveBuildingsForLocationSync();

    int updateBuildingLocationIfMissing(@Param("buildingId") Long buildingId,
                                        @Param("longitude") BigDecimal longitude,
                                        @Param("latitude") BigDecimal latitude);

    List<LocatedBuildingCoordinate> selectLocatedActiveBuildings();

    List<CommunityCentroidCandidate> selectMissingCommunityCentroidsFromBuildings();

    int updateCommunityCentroidIfMissing(@Param("communityId") Long communityId,
                                         @Param("longitude") BigDecimal longitude,
                                         @Param("latitude") BigDecimal latitude);
}
