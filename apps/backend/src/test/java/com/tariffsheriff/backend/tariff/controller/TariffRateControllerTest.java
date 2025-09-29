package com.tariffsheriff.backend.tariff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.service.TariffCalculationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TariffRateController.class)
@AutoConfigureMockMvc(addFilters = false)
class TariffRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TariffCalculationService tariffCalculationService;

    @Test
    void calculateAcceptsLowercaseFob() throws Exception {
        when(tariffCalculationService.calculateTariffRate(any())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(post("/api/tariff-rate/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"importer_id\":1," +
                    "\"origin_id\":2," +
                    "\"hsCode\":1," +
                    "\"quantity\":100," +
                    "\"totalValue\":500000," +
                    "\"materialCost\":200000," +
                    "\"labourCost\":80000," +
                    "\"overheadCost\":50000," +
                    "\"profit\":30000," +
                    "\"otherCosts\":20000," +
                    "\"fob\":400000," +
                    "\"nonOriginValue\":50000}"))
            .andExpect(status().isOk());

        ArgumentCaptor<TariffRateRequestDto> captor = ArgumentCaptor.forClass(TariffRateRequestDto.class);
        verify(tariffCalculationService).calculateTariffRate(captor.capture());
        assertThat(captor.getValue().getFob()).isEqualByComparingTo(new BigDecimal("400000"));
    }

    @Test
    void calculateAcceptsUppercaseFobAlias() throws Exception {
        when(tariffCalculationService.calculateTariffRate(any())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(post("/api/tariff-rate/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"importer_id\":1," +
                    "\"origin_id\":2," +
                    "\"hsCode\":1," +
                    "\"quantity\":100," +
                    "\"totalValue\":500000," +
                    "\"materialCost\":200000," +
                    "\"labourCost\":80000," +
                    "\"overheadCost\":50000," +
                    "\"profit\":30000," +
                    "\"otherCosts\":20000," +
                    "\"FOB\":250000," +
                    "\"nonOriginValue\":50000}"))
            .andExpect(status().isOk());

        ArgumentCaptor<TariffRateRequestDto> captor = ArgumentCaptor.forClass(TariffRateRequestDto.class);
        verify(tariffCalculationService).calculateTariffRate(captor.capture());
        assertThat(captor.getValue().getFob()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    void calculateReturnsBadRequestForUnreadablePayload() throws Exception {
        mockMvc.perform(post("/api/tariff-rate/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"importer_id\":1," +
                    "\"fob\":\"not-a-number\"}"))
            .andExpect(status().isBadRequest());

        verify(tariffCalculationService, never()).calculateTariffRate(any());
    }
}
