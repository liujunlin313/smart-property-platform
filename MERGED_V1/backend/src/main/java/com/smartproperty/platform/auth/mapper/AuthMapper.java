package com.smartproperty.platform.auth.mapper;

import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.auth.model.RefreshSessionRow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuthMapper {
    AccountRow findAccountByUsername(@Param("username") String username);

    AccountRow findAccountById(@Param("accountId") Long accountId);

    List<String> findGlobalRoleCodes(@Param("accountId") Long accountId);

    Integer findRoleId(@Param("roleCode") String roleCode);

    int insertAccount(@Param("username") String username, @Param("passwordHash") String passwordHash);

    int addGlobalRole(@Param("accountId") Long accountId, @Param("roleId") Integer roleId);

    int reconcileBootstrapAccount(@Param("accountId") Long accountId,
                                  @Param("passwordHash") String passwordHash);

    int recordLoginFailure(@Param("accountId") Long accountId,
                           @Param("attempts") int attempts,
                           @Param("status") String status,
                           @Param("lockedUntil") LocalDateTime lockedUntil);

    int recordLoginSuccess(@Param("accountId") Long accountId);

    int clearExpiredLock(@Param("accountId") Long accountId);

    int insertRefreshSession(@Param("accountId") Long accountId,
                             @Param("tokenHash") String tokenHash,
                             @Param("expiresAt") LocalDateTime expiresAt,
                             @Param("userAgent") String userAgent);

    RefreshSessionRow findRefreshSessionForUpdate(@Param("tokenHash") String tokenHash);

    int rotateRefreshSession(@Param("sessionId") Long sessionId,
                             @Param("replacementHash") String replacementHash,
                             @Param("usedAt") LocalDateTime usedAt);

    int revokeRefreshSession(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);

    int revokeAllRefreshSessions(@Param("accountId") Long accountId, @Param("revokedAt") LocalDateTime revokedAt);
}
