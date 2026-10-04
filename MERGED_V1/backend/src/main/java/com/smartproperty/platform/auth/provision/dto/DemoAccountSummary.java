package com.smartproperty.platform.auth.provision.dto;

import java.util.List;

/** Contains no password or token material. */
public record DemoAccountSummary(String username, String roleCode, Long personId, String residentKind,
                                 List<String> communityNames, boolean created) { }
