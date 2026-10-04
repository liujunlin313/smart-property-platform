package com.smartproperty.platform.auth.provision.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.auth.provision.dto.DemoAccountProvisionReport;
import com.smartproperty.platform.auth.provision.dto.DemoAccountSummary;
import com.smartproperty.platform.auth.provision.mapper.DemoAccountProvisionMapper;
import com.smartproperty.platform.auth.provision.model.CommunityProvisionRow;
import com.smartproperty.platform.auth.provision.model.ResidentProvisionCandidate;
import com.smartproperty.platform.auth.provision.service.DemoAccountProvisionService;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Controlled local/demo provisioning; this is not a public registration mechanism. */
@Service
public class DemoAccountProvisionServiceImpl implements DemoAccountProvisionService {
    private static final String INITIAL_DEMO_PASSWORD = "123456";
    private static final String PROPERTY_STAFF = "PROPERTY_STAFF";
    private static final String RESIDENT_ROLE = "PERSON_USER";
    private static final int RESIDENT_LIMIT = 50;
    private final AuthorizationService authorizationService;
    private final DemoAccountProvisionMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountProvisionServiceImpl(AuthorizationService authorizationService, DemoAccountProvisionMapper mapper, PasswordEncoder passwordEncoder) {
        this.authorizationService = authorizationService; this.mapper = mapper; this.passwordEncoder = passwordEncoder;
    }

    @Override @Transactional public DemoAccountProvisionReport provisionStageGDemoAccounts() {
        var actor = authorizationService.requireSystemAdmin();
        List<CommunityProvisionRow> communities = mapper.selectActiveCommunities();
        if (communities.size() < 4) throw new BusinessException(HttpStatus.CONFLICT, "STAGE_G_COMMUNITY_SAMPLE_INSUFFICIENT", "Stage G 需要至少四个有效小区来建立工作人员 Scope 样本");
        Integer staffRoleId = requiredRole(PROPERTY_STAFF); Integer residentRoleId = requiredRole(RESIDENT_ROLE);
        List<DemoAccountSummary> summaries = new ArrayList<>();
        int[] staffCounts = provisionStaffSamples(actor.accountId(), staffRoleId, communities, summaries);
        List<ResidentProvisionCandidate> candidates = mapper.selectResidentCandidates(1000);
        int residentExisting = (int) candidates.stream().filter(ResidentProvisionCandidate::residentAccount).count();
        List<ResidentProvisionCandidate> selected = selectResidentCohort(candidates, residentExisting);
        int residentCreated = 0;
        for (ResidentProvisionCandidate candidate : selected) {
            String username = nextResidentUsername(candidate.fullName());
            mapper.insertPersonAccount(username, passwordEncoder.encode(INITIAL_DEMO_PASSWORD), candidate.personId());
            AccountRow account = mapper.findAccountByUsername(username);
            mapper.addRole(account.accountId(), residentRoleId);
            summaries.add(new DemoAccountSummary(account.username(), RESIDENT_ROLE, candidate.personId(), candidate.residentKind(), List.of(), true));
            residentCreated++;
        }
        int totalResidentAccounts = residentExisting + residentCreated;
        return new DemoAccountProvisionReport(staffCounts[0], staffCounts[1], residentCreated, residentExisting,
                Math.max(0, RESIDENT_LIMIT - totalResidentAccounts), List.copyOf(summaries));
    }

    private int[] provisionStaffSamples(Long assignedByAccountId, Integer staffRoleId, List<CommunityProvisionRow> communities, List<DemoAccountSummary> summaries) {
        List<StaffPlan> plans = List.of(new StaffPlan("staff_scope_a", List.of(communities.get(0))),
                new StaffPlan("staff_scope_b", List.of(communities.get(1))),
                new StaffPlan("staff_scope_c", List.of(communities.get(0), communities.get(2))),
                new StaffPlan("staff_scope_d", List.of(communities.get(3))));
        int created = 0; int existing = 0;
        for (StaffPlan plan : plans) {
            AccountRow account = mapper.findAccountByUsername(plan.username()); boolean newAccount = false;
            if (account == null) { mapper.insertStaffAccount(plan.username(), passwordEncoder.encode(INITIAL_DEMO_PASSWORD)); account = mapper.findAccountByUsername(plan.username()); newAccount = true; created++; }
            else existing++;
            mapper.addRole(account.accountId(), staffRoleId);
            for (CommunityProvisionRow community : plan.communities()) mapper.activateScope(account.accountId(), community.communityId(), assignedByAccountId);
            summaries.add(new DemoAccountSummary(plan.username(), PROPERTY_STAFF, null, "PROPERTY_STAFF", plan.communities().stream().map(CommunityProvisionRow::communityName).toList(), newAccount));
        }
        return new int[]{created, existing};
    }
    private Integer requiredRole(String roleCode) { Integer roleId = mapper.selectRoleId(roleCode); if (roleId == null) throw new BusinessException(HttpStatus.CONFLICT, "STAGE_G_ROLE_MISSING", "缺少角色：" + roleCode); return roleId; }
    private List<ResidentProvisionCandidate> selectResidentCohort(List<ResidentProvisionCandidate> candidates, int existingCount) {
        List<ResidentProvisionCandidate> selected = new ArrayList<>();
        if (existingCount == 0) {
            addFirst(selected, candidates, candidate -> candidate.ownerTenantPair() && candidate.ownerFlag());
            addFirst(selected, candidates, candidate -> candidate.ownerTenantPair() && !candidate.ownerFlag());
            addFirst(selected, candidates, candidate -> "MEMBER".equals(candidate.residenceRole()));
            addFirst(selected, candidates, candidate -> "RESPONSIBLE".equals(candidate.residenceRole()));
            addFirst(selected, candidates, candidate -> "TENANT_OCCUPIED".equals(candidate.occupancyStatus()) && !candidate.ownerFlag());
            addFirst(selected, candidates, ResidentProvisionCandidate::ownerFlag);
            for (ResidentProvisionCandidate candidate : candidates) { if (selected.size() >= RESIDENT_LIMIT) break; if (candidate.accountId() == null && !selected.contains(candidate)) selected.add(candidate); }
            return selected;
        }
        boolean hasTenant = candidates.stream().anyMatch(candidate -> candidate.residentAccount() && !candidate.ownerFlag() && "TENANT_OCCUPIED".equals(candidate.occupancyStatus()));
        boolean hasMember = candidates.stream().anyMatch(candidate -> candidate.residentAccount() && "MEMBER".equals(candidate.residenceRole()));
        boolean hasResponsible = candidates.stream().anyMatch(candidate -> candidate.residentAccount() && "RESPONSIBLE".equals(candidate.residenceRole()));
        if (!mapper.hasResidentOwnerTenantPair()) {
            Long tenantPersonId = mapper.selectUnprovisionedTenantPersonIdForResidentOwner();
            if (tenantPersonId != null) candidates.stream().filter(candidate -> tenantPersonId.equals(candidate.personId())).findFirst().ifPresent(selected::add);
        }
        if (!hasTenant) addFirst(selected, candidates, candidate -> "TENANT_OCCUPIED".equals(candidate.occupancyStatus()) && !candidate.ownerFlag());
        if (!hasMember) addFirst(selected, candidates, candidate -> "MEMBER".equals(candidate.residenceRole()));
        if (!hasResponsible) addFirst(selected, candidates, candidate -> "RESPONSIBLE".equals(candidate.residenceRole()));
        return selected;
    }
    private void addFirst(List<ResidentProvisionCandidate> selected, List<ResidentProvisionCandidate> candidates, Predicate<ResidentProvisionCandidate> predicate) {
        candidates.stream().filter(candidate -> candidate.accountId() == null).filter(predicate).filter(candidate -> !selected.contains(candidate)).findFirst().ifPresent(selected::add);
    }
    private String nextResidentUsername(String fullName) {
        String base = PinyinInitials.from(fullName); if (base.isBlank()) base = "resident"; base = base.length() > 52 ? base.substring(0, 52) : base;
        if (mapper.findAccountByUsername(base) == null) return base;
        for (int sequence = 2; sequence <= 9999; sequence++) { String candidate = base + String.format(Locale.ROOT, "%03d", sequence); if (mapper.findAccountByUsername(candidate) == null) return candidate; }
        throw new BusinessException(HttpStatus.CONFLICT, "STAGE_G_USERNAME_EXHAUSTED", "居民演示用户名编号已耗尽");
    }
    private record StaffPlan(String username, List<CommunityProvisionRow> communities) { }

    /** Deterministic GB2312-style initials without a new pinyin dependency. */
    static final class PinyinInitials {
        private static final int[] BOUNDARIES = {-20319,-20284,-19776,-19219,-18711,-18527,-18240,-17923,-17418,-16475,-16213,-15641,-15166,-14923,-14915,-14631,-14150,-14091,-13319,-12839,-12557,-11848,-11056,-10247};
        private static final char[] INITIALS = "ABCDEFGHJKLMNOPQRSTWXYZ".toCharArray();
        static String from(String source) { if (source == null) return ""; StringBuilder result = new StringBuilder(); for (char character : source.trim().toCharArray()) { if (Character.isLetterOrDigit(character) && character < 128) result.append(Character.toLowerCase(character)); else if (Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN) result.append(initial(character)); } return result.toString(); }
        private static char initial(char character) { byte[] bytes = String.valueOf(character).getBytes(Charset.forName("GB2312")); if (bytes.length < 2) return 'x'; int code = (bytes[0] & 0xff) * 256 + (bytes[1] & 0xff) - 65536; for (int index = Math.min(BOUNDARIES.length, INITIALS.length) - 1; index >= 0; index--) if (code >= BOUNDARIES[index]) return Character.toLowerCase(INITIALS[index]); return 'x'; }
    }
}
