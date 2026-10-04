package com.smartproperty.platform.notification.charge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/** Builds honest, recipient-local charge data without inventing item-level payment allocation. */
@Component
public class ChargeNoticeComposer {
    private final ChargeNoticeMapper mapper;
    private final ObjectMapper objectMapper;

    public ChargeNoticeComposer(ChargeNoticeMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public Map<Long, String> compose(List<Long> personIds, Long feeTypeId, CommunityDataScope scope) {
        if (feeTypeId != null && !mapper.existsActiveFeeType(feeTypeId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FEE_TYPE", "收费类别不存在或未启用");
        }
        Map<Long, List<ChargeNoticeRow>> rowsByPerson = new HashMap<>();
        for (ChargeNoticeRow row : mapper.selectDueRows(personIds, scope)) {
            rowsByPerson.computeIfAbsent(row.personId(), ignored -> new ArrayList<>()).add(row);
        }
        Map<Long, String> payloads = new LinkedHashMap<>();
        for (Long personId : personIds) {
            payloads.put(personId, serialize(personId, feeTypeId, rowsByPerson.getOrDefault(personId, List.of())));
        }
        return payloads;
    }

    private String serialize(Long personId, Long selectedFeeTypeId, List<ChargeNoticeRow> rows) {
        Map<Long, List<ChargeNoticeRow>> byBill = new LinkedHashMap<>();
        for (ChargeNoticeRow row : rows) byBill.computeIfAbsent(row.billId(), ignored -> new ArrayList<>()).add(row);
        Map<Long, Map<String, Object>> properties = new LinkedHashMap<>();
        BigDecimal totalDueBillOutstanding = BigDecimal.ZERO;
        BigDecimal exactSelectedFeeOutstanding = BigDecimal.ZERO;
        BigDecimal unallocatedSelectedBillOutstanding = BigDecimal.ZERO;

        for (List<ChargeNoticeRow> billRows : byBill.values()) {
            ChargeNoticeRow first = billRows.getFirst();
            BigDecimal billOutstanding = first.billOutstandingAmount();
            totalDueBillOutstanding = totalDueBillOutstanding.add(billOutstanding);
            Map<String, Object> property = properties.computeIfAbsent(first.propertyUnitId(), ignored -> property(first));
            property.put("totalDueBillOutstanding", money(property.get("totalDueBillOutstanding")).add(billOutstanding));
            boolean itemLevelExact = !"PARTIALLY_PAID".equals(first.billStatus()) || billRows.size() == 1;
            boolean selectedFeeAppears = selectedFeeTypeId == null || billRows.stream().anyMatch(row -> selectedFeeTypeId.equals(row.feeTypeId()));
            if (itemLevelExact) {
                for (ChargeNoticeRow row : billRows) {
                    if (selectedFeeTypeId != null && !selectedFeeTypeId.equals(row.feeTypeId())) continue;
                    BigDecimal remaining = "PARTIALLY_PAID".equals(first.billStatus()) ? billOutstanding : row.itemAmount();
                    exactSelectedFeeOutstanding = exactSelectedFeeOutstanding.add(remaining);
                    @SuppressWarnings("unchecked") List<Map<String, Object>> feeItems = (List<Map<String, Object>>) property.get("feeItems");
                    feeItems.add(feeItem(row, remaining));
                }
            } else if (selectedFeeAppears) {
                unallocatedSelectedBillOutstanding = unallocatedSelectedBillOutstanding.add(billOutstanding);
                @SuppressWarnings("unchecked") List<Map<String, Object>> unallocatedBills = (List<Map<String, Object>>) property.get("unallocatedBills");
                unallocatedBills.add(Map.of("billId", first.billId(), "billNo", first.billNo(), "outstandingAmount", billOutstanding,
                        "reason", "该账单已部分支付且含多个收费项目，现有支付模型无法无假设分摊到具体费用项"));
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationType", "CHARGE");
        payload.put("personId", personId);
        payload.put("selectedFeeTypeId", selectedFeeTypeId);
        payload.put("totalDueBillOutstanding", totalDueBillOutstanding);
        payload.put("exactSelectedFeeOutstanding", exactSelectedFeeOutstanding);
        payload.put("unallocatedSelectedBillOutstanding", unallocatedSelectedBillOutstanding);
        payload.put("properties", new ArrayList<>(properties.values()));
        return write(payload);
    }

    private Map<String, Object> property(ChargeNoticeRow row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("propertyUnitId", row.propertyUnitId());
        result.put("propertyNo", row.propertyNo());
        result.put("communityName", row.communityName());
        result.put("buildingNo", row.buildingNo());
        result.put("totalDueBillOutstanding", BigDecimal.ZERO);
        result.put("feeItems", new ArrayList<>());
        result.put("unallocatedBills", new ArrayList<>());
        return result;
    }

    private Map<String, Object> feeItem(ChargeNoticeRow row, BigDecimal remaining) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feeTypeId", row.feeTypeId());
        result.put("feeCode", row.feeCode());
        result.put("feeName", row.feeName());
        result.put("remainingAmount", remaining);
        return result;
    }

    private BigDecimal money(Object value) { return value instanceof BigDecimal amount ? amount : BigDecimal.ZERO; }
    private String write(Map<String, Object> payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException exception) { throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "CHARGE_PAYLOAD_SERIALIZATION_FAILED", "收费通知数据生成失败"); }
    }
}
