package com.smartproperty.platform.auth.provision.dto;

import java.util.List;

/** Safe, credential-free result for the local Stage G demo-account provisioning action. */
public record DemoAccountProvisionReport(int staffCreated, int staffExisting, int residentCreated,
                                         int residentExisting, int residentSkipped,
                                         List<DemoAccountSummary> accounts) { }
