package com.smartproperty.platform.notification.charge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.access.CommunityDataScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChargeNoticeComposerTest {

    @Test
    void keepsPartiallyPaidMultiItemBillAtBillLevelInsteadOfInventingFeeAllocation() throws Exception {
        ChargeNoticeMapper mapper = mock(ChargeNoticeMapper.class);
        when(mapper.existsActiveFeeType(1L)).thenReturn(true);
        when(mapper.selectDueRows(anyList(), any())).thenReturn(List.of(
                new ChargeNoticeRow(8L, 20L, "502", "演示小区", "1", 30L, "B-30", "PARTIALLY_PAID", new BigDecimal("200.00"), 1L, "PROPERTY", "物业费", new BigDecimal("300.00")),
                new ChargeNoticeRow(8L, 20L, "502", "演示小区", "1", 30L, "B-30", "PARTIALLY_PAID", new BigDecimal("200.00"), 2L, "WATER", "水费", new BigDecimal("100.00"))
        ));
        ChargeNoticeComposer composer = new ChargeNoticeComposer(mapper, new ObjectMapper());

        String payload = composer.compose(List.of(8L), 1L, new CommunityDataScope(true, List.of())).get(8L);
        var json = new ObjectMapper().readTree(payload);

        assertEquals(0, json.path("totalDueBillOutstanding").decimalValue().compareTo(new BigDecimal("200.00")));
        assertEquals(0, json.path("exactSelectedFeeOutstanding").decimalValue().compareTo(BigDecimal.ZERO));
        assertEquals(0, json.path("unallocatedSelectedBillOutstanding").decimalValue().compareTo(new BigDecimal("200.00")));
        assertEquals(0, json.path("properties").get(0).path("feeItems").size());
        assertEquals(1, json.path("properties").get(0).path("unallocatedBills").size());
    }
}
