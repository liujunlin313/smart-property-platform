package com.smartproperty.platform.auth.service;

import com.smartproperty.platform.auth.mapper.AuthMapper;
import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.infrastructure.security.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AccountBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AccountBootstrapRunner.class);

    private final AuthMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    public AccountBootstrapRunner(AuthMapper mapper,
                                  PasswordEncoder passwordEncoder,
                                  BootstrapProperties properties) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrap(properties.adminUsername(), properties.adminPassword(), "SYSTEM_ADMIN");
        bootstrap(properties.viewerUsername(), properties.viewerPassword(), "DEMO_VIEWER");
    }

    private void bootstrap(String username, String password, String roleCode) {
        boolean usernameBlank = username == null || username.isBlank();
        boolean passwordBlank = password == null || password.isBlank();
        if (usernameBlank && passwordBlank) {
            return;
        }
        if (usernameBlank || passwordBlank) {
            throw new IllegalStateException("Bootstrap username and password must be supplied together for " + roleCode);
        }
        if (password.length() < 10) {
            throw new IllegalStateException("Bootstrap password must contain at least 10 characters for " + roleCode);
        }
        AccountRow account = mapper.findAccountByUsername(username.trim());
        if (account == null) {
            mapper.insertAccount(username.trim(), passwordEncoder.encode(password));
            account = mapper.findAccountByUsername(username.trim());
            log.info("Created bootstrap account '{}' with role {}", username.trim(), roleCode);
        } else if (properties.reconcileLocal()) {
            /*
             * Phase 2A Stabilization - local bootstrap accounts are deterministic.
             * Ordinary database accounts are never modified by this path.
             */
            mapper.reconcileBootstrapAccount(account.accountId(), passwordEncoder.encode(password));
            mapper.revokeAllRefreshSessions(account.accountId(), LocalDateTime.now());
            account = mapper.findAccountById(account.accountId());
            log.info("Reconciled local bootstrap account '{}' with role {}; credentials remain hidden",
                    username.trim(), roleCode);
        }
        Integer roleId = mapper.findRoleId(roleCode);
        if (roleId == null) {
            throw new IllegalStateException("Required role is missing: " + roleCode);
        }
        mapper.addGlobalRole(account.accountId(), roleId);
    }
}
