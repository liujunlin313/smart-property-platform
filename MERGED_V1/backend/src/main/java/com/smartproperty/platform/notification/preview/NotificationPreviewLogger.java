package com.smartproperty.platform.notification.preview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.notification.dto.NotificationRecipientCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/** Local-only observability; uses already-created recipient payloads and never queries the database. */
@Component
@Profile({"local", "dev"})
public class NotificationPreviewLogger {
    private static final Logger log = LoggerFactory.getLogger("NOTIFICATION_PREVIEW");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?i)(?<![0-9x])\\d{17}[0-9x](?![0-9x])");
    private final ObjectMapper objectMapper;

    public NotificationPreviewLogger(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public void preview(Long notificationId, String category, String content, List<NotificationRecipientCommand> recipients) {
        int sampleCount = Math.min(10, recipients.size());
        log.info("notificationId={} category={} recipientCount={} sampleCount={}", notificationId, category, recipients.size(), sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            NotificationRecipientCommand recipient = recipients.get(index);
            if ("BILLING".equals(category)) logCharge(notificationId, index + 1, recipient);
            else log.info("notificationId={} sample={} personId={} recipientRole={} renderedMessage={}",
                    notificationId, index + 1, recipient.personId(), recipient.recipientRole(), sanitize(content));
        }
    }

    private void logCharge(Long notificationId, int sample, NotificationRecipientCommand recipient) {
        try {
            JsonNode payload = objectMapper.readTree(recipient.payloadJson());
            String properties = payload.path("properties").isArray()
                    ? payload.path("properties").valueStream().map(this::propertySummary).reduce((left, right) -> left + " | " + right).orElse("无当前到期账单")
                    : "无当前到期账单";
            log.info("notificationId={} sample={} personId={} recipientRole={} totalDueBillOutstanding={} exactSelectedFeeOutstanding={} unallocatedSelectedBillOutstanding={} properties={}",
                    notificationId, sample, recipient.personId(), recipient.recipientRole(),
                    payload.path("totalDueBillOutstanding").asText(), payload.path("exactSelectedFeeOutstanding").asText(),
                    payload.path("unallocatedSelectedBillOutstanding").asText(), properties);
        } catch (Exception ignored) {
            log.warn("notificationId={} sample={} personId={} recipientRole={} chargePayload=UNREADABLE",
                    notificationId, sample, recipient.personId(), recipient.recipientRole());
        }
    }

    private String propertySummary(JsonNode property) {
        StringBuilder result = new StringBuilder();
        String building = property.path("buildingNo").asText("-");
        String buildingLabel = building.endsWith("号楼") ? building : building + "号楼";
        result.append(property.path("communityName").asText("-")).append(" · ")
                .append(buildingLabel).append(" · ")
                .append(property.path("propertyNo").asText("-"));
        if (property.path("feeItems").isArray() && !property.path("feeItems").isEmpty()) {
            result.append(" [");
            boolean first = true;
            for (JsonNode item : property.path("feeItems")) {
                if (!first) result.append(", ");
                result.append(item.path("feeName").asText("-")).append(":").append(item.path("remainingAmount").asText("0"));
                first = false;
            }
            result.append("]");
        }
        if (property.path("unallocatedBills").isArray() && !property.path("unallocatedBills").isEmpty()) result.append(" [部分支付账单未按费用项分摊]");
        return result.toString();
    }

    private String sanitize(String value) {
        String normalized = value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
        normalized = PHONE.matcher(normalized).replaceAll("[REDACTED-PHONE]");
        normalized = ID_CARD.matcher(normalized).replaceAll("[REDACTED-ID]");
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "…";
    }
}
