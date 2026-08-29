package net.wowdev.ecommerce.payments.service;

import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.entity.CustomerEntity;
import net.wowdev.ecommerce.domain.enums.CustomerStatus;
import net.wowdev.ecommerce.payments.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCustomerServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private CustomerRepository customerRepository;
    private DefaultCustomerService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        service = new DefaultCustomerService(customerRepository);
    }

    @Test
    void findsCustomerById() {
        final CustomerEntity entity = customerEntity("Original");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(entity));

        final CustomerDTO result = service.findById(CUSTOMER_ID);

        assertThat(result.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.getFirstName()).isEqualTo("Original");
        assertThat(result.getEmail()).isEqualTo("original@example.com");
        verify(customerRepository).findById(CUSTOMER_ID);
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(CUSTOMER_ID))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found: " + CUSTOMER_ID);
    }

    @Test
    void insertsReplicaWhenCustomerDoesNotExist() {
        final CustomerDTO customer = customerDto("New");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(customerRepository.save(any(CustomerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final CustomerDTO result = service.updateReplicaEntity(customer);

        assertThat(result).isEqualTo(customer);
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    void updatesAllReplicaFieldsWhenCustomerAlreadyExists() {
        final CustomerDTO customer = customerDto("Updated");
        final CustomerEntity existing = customerEntity("Old");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(CustomerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final CustomerDTO result = service.updateReplicaEntity(customer);

        assertThat(result).isEqualTo(customer);
        assertThat(existing.getFirstName()).isEqualTo(customer.getFirstName());
        assertThat(existing.getLastName()).isEqualTo(customer.getLastName());
        assertThat(existing.getEmail()).isEqualTo(customer.getEmail());
        assertThat(existing.getCustomerStatus()).isEqualTo(customer.getCustomerStatus());
        assertThat(existing.getAddressLine1()).isEqualTo(customer.getAddressLine1());
        assertThat(existing.getAddressLine2()).isEqualTo(customer.getAddressLine2());
        assertThat(existing.getCity()).isEqualTo(customer.getCity());
        assertThat(existing.getStateProvince()).isEqualTo(customer.getStateProvince());
        assertThat(existing.getPostalCode()).isEqualTo(customer.getPostalCode());
        assertThat(existing.getCountry()).isEqualTo(customer.getCountry());
        assertThat(existing.getCreatedAt()).isEqualTo(customer.getCreatedAt());
        assertThat(existing.getModifiedAt()).isEqualTo(customer.getModifiedAt());
        verify(customerRepository).save(existing);
    }

    private static CustomerDTO customerDto(final String firstName) {
        final CustomerDTO customer = new CustomerDTO();
        customer.setId(CUSTOMER_ID);
        customer.setFirstName(firstName);
        customer.setLastName("Customer");
        customer.setEmail(firstName.toLowerCase() + "@example.com");
        customer.setCustomerStatus(CustomerStatus.ACTIVE);
        customer.setPaymentMethods(List.of());
        customer.setAddressLine1("1 Main Street");
        customer.setAddressLine2("Apt 2");
        customer.setCity("Sao Paulo");
        customer.setStateProvince("SP");
        customer.setPostalCode("01000-000");
        customer.setCountry("Brazil");
        customer.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        customer.setModifiedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return customer;
    }

    private static CustomerEntity customerEntity(final String firstName) {
        final CustomerDTO customer = customerDto(firstName);
        return new CustomerEntity(customer.getId(), customer.getFirstName(), customer.getLastName(),
                customer.getEmail(), customer.getCustomerStatus(), List.of(), customer.getAddressLine1(),
                customer.getAddressLine2(), customer.getCity(), customer.getStateProvince(),
                customer.getPostalCode(), customer.getCountry(), customer.getCreatedAt(), customer.getModifiedAt());
    }
}
