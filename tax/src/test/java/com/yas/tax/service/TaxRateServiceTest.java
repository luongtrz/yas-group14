package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private TaxRateService taxRateService;

    private TaxClass taxClass;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder().id(1L).name("VAT").build();
        taxRate = TaxRate.builder()
            .id(1L)
            .rate(10.0)
            .zipCode("70000")
            .stateOrProvinceId(1L)
            .countryId(1L)
            .taxClass(taxClass)
            .build();
    }

    @Nested
    class FindAll {

        @Test
        void shouldReturnAllTaxRates() {
            when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

            List<TaxRateVm> result = taxRateService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().rate()).isEqualTo(10.0);
        }

        @Test
        void whenEmpty_shouldReturnEmptyList() {
            when(taxRateRepository.findAll()).thenReturn(List.of());

            List<TaxRateVm> result = taxRateService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void whenTaxRateExists_shouldReturnVm() {
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));

            TaxRateVm result = taxRateService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.rate()).isEqualTo(10.0);
            assertThat(result.taxClassId()).isEqualTo(1L);
        }

        @Test
        void whenTaxRateNotFound_shouldThrowNotFoundException() {
            when(taxRateRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxRateService.findById(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class CreateTaxRate {

        @Test
        void whenTaxClassExists_shouldCreateSuccessfully() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 1L, 1L, 1L);
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
            when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

            TaxRate result = taxRateService.createTaxRate(postVm);

            assertThat(result.getRate()).isEqualTo(10.0);
            verify(taxRateRepository).save(any(TaxRate.class));
        }

        @Test
        void whenTaxClassNotFound_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 999L, 1L, 1L);
            when(taxClassRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> taxRateService.createTaxRate(postVm))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void whenOptionalFieldsNull_shouldCreateSuccessfully() {
            TaxRatePostVm postVm = new TaxRatePostVm(5.0, null, 1L, null, 1L);
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
            when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(inv -> inv.getArgument(0));

            TaxRate result = taxRateService.createTaxRate(postVm);

            assertThat(result.getZipCode()).isNull();
            assertThat(result.getStateOrProvinceId()).isNull();
        }
    }

    @Nested
    class UpdateTaxRate {

        @Test
        void whenValidUpdate_shouldUpdateAllFields() {
            TaxRatePostVm postVm = new TaxRatePostVm(15.0, "80000", 1L, 2L, 2L);
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);

            taxRateService.updateTaxRate(postVm, 1L);

            assertThat(taxRate.getRate()).isEqualTo(15.0);
            assertThat(taxRate.getZipCode()).isEqualTo("80000");
            assertThat(taxRate.getCountryId()).isEqualTo(2L);
            verify(taxRateRepository).save(taxRate);
        }

        @Test
        void whenTaxRateNotFound_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 1L, 1L, 1L);
            when(taxRateRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 999L))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void whenTaxClassNotFound_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "70000", 999L, 1L, 1L);
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
            when(taxClassRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 1L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void whenTaxRateExists_shouldDeleteSuccessfully() {
            when(taxRateRepository.existsById(1L)).thenReturn(true);

            taxRateService.delete(1L);

            verify(taxRateRepository).deleteById(1L);
        }

        @Test
        void whenTaxRateNotFound_shouldThrowNotFoundException() {
            when(taxRateRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> taxRateService.delete(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetTaxPercent {

        @Test
        void whenTaxPercentExists_shouldReturnValue() {
            when(taxRateRepository.getTaxPercent(1L, 1L, "70000", 1L)).thenReturn(10.0);

            double result = taxRateService.getTaxPercent(1L, 1L, 1L, "70000");

            assertThat(result).isEqualTo(10.0);
        }

        @Test
        void whenTaxPercentIsNull_shouldReturnZero() {
            when(taxRateRepository.getTaxPercent(anyLong(), anyLong(), anyString(), anyLong()))
                .thenReturn(null);

            double result = taxRateService.getTaxPercent(1L, 1L, 1L, "70000");

            assertThat(result).isZero();
        }
    }

    @Nested
    class GetBulkTaxRate {

        @Test
        void shouldReturnTaxRatesForGivenIds() {
            when(taxRateRepository.getBatchTaxRates(eq(1L), eq(1L), eq("70000"), anySet()))
                .thenReturn(List.of(taxRate));

            List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 1L, 1L, "70000");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().rate()).isEqualTo(10.0);
        }

        @Test
        void whenNoMatches_shouldReturnEmptyList() {
            when(taxRateRepository.getBatchTaxRates(anyLong(), anyLong(), anyString(), anySet()))
                .thenReturn(List.of());

            List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(999L), 1L, 1L, "00000");

            assertThat(result).isEmpty();
        }
    }
}
