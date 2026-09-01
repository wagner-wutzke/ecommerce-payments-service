package net.wowdev.ecommerce.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.service.PaymentNotFoundException;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private PaymentService service;

  @Test
  void supportsPaymentEndpoints() throws Exception {
    final PaymentDTO payment = TestFixtures.paymentDto();
    when(service.findById(payment.getId())).thenReturn(payment);
    when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(payment)));
    when(service.create(any())).thenReturn(payment);
    when(service.update(any(), any())).thenReturn(payment);
    mockMvc.perform(get("/api/v1/payments/{id}", payment.getId())).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/payments")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionId\":\"TX-1\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/payments/" + payment.getId()));
    mockMvc
        .perform(
            put("/api/v1/payments/{id}", payment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionId\":\"TX-2\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(delete("/api/v1/payments/{id}", payment.getId()))
        .andExpect(status().isNoContent());
  }

  @Test
  void rejectsBadPaginationAndMapsErrors() throws Exception {
    mockMvc.perform(get("/api/v1/payments?page=-1")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/payments?pageSize=0")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/payments?pageSize=101")).andExpect(status().isBadRequest());
    final PaymentController controller = new PaymentController(service);
    org.assertj.core.api.Assertions.assertThat(
            controller.notFound(new PaymentNotFoundException("missing")).getStatusCode().value())
        .isEqualTo(404);
    org.assertj.core.api.Assertions.assertThat(
            controller.badRequest(new IllegalArgumentException("bad")).getStatusCode().value())
        .isEqualTo(400);
  }
}
