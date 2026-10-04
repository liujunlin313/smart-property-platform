package com.smartproperty.platform.auth.provision.mapper;

import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.auth.provision.model.CommunityProvisionRow;
import com.smartproperty.platform.auth.provision.model.ResidentProvisionCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DemoAccountProvisionMapper {
    List<CommunityProvisionRow> selectActiveCommunities();
    Integer selectRoleId(@Param("roleCode") String roleCode);
    AccountRow findAccountByUsername(@Param("username") String username);
    AccountRow findAccountByPersonId(@Param("personId") Long personId);
    int insertPersonAccount(@Param("username") String username, @Param("passwordHash") String passwordHash, @Param("personId") Long personId);
    int insertStaffAccount(@Param("username") String username, @Param("passwordHash") String passwordHash);
    int addRole(@Param("accountId") Long accountId, @Param("roleId") Integer roleId);
    int activateScope(@Param("accountId") Long accountId, @Param("communityId") Long communityId, @Param("assignedByAccountId") Long assignedByAccountId);
    List<ResidentProvisionCandidate> selectResidentCandidates(@Param("limit") int limit);
    boolean hasResidentOwnerTenantPair();
    Long selectUnprovisionedTenantPersonIdForResidentOwner();
}
