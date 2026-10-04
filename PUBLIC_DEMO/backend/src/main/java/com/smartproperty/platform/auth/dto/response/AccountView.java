package com.smartproperty.platform.auth.dto.response;

import java.util.List;

public record AccountView(String accountId, String username, List<String> roles) {
}
