package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    private TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder().id(1L).name("VAT").build();
    }

    @Nested
    class FindAllTaxClasses {

        @Test
        void shouldReturnAllTaxClassesSortedByName() {
            TaxClass taxClass2 = TaxClass.builder().id(2L).name("GST").build();
            when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(taxClass2, taxClass));

            List<TaxClassVm> result = taxClassService.findAllTaxClasses();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("GST");
            assertThat(result.get(1).name()).isEqualTo("VAT");
        }

        @Test
        void whenNoTaxClasses_shouldReturnEmptyList() {
            when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of());

            List<TaxClassVm> result = taxClassService.findAllTaxClasses();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void whenTaxClassExists_shouldReturnTaxClassVm() {
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

            TaxClassVm result = taxClassService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("VAT");
        }

        @Test
        void whenTaxClassNotFound_shouldThrowNotFoundException() {
            when(taxClassRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxClassService.findById(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Create {

        @Test
        void whenNameIsUnique_shouldCreateSuccessfully() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
            when(taxClassRepository.existsByName("VAT")).thenReturn(false);
            when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

            TaxClass result = taxClassService.create(postVm);

            assertThat(result.getName()).isEqualTo("VAT");
            verify(taxClassRepository).save(any(TaxClass.class));
        }

        @Test
        void whenNameIsDuplicate_shouldThrowDuplicatedException() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
            when(taxClassRepository.existsByName("VAT")).thenReturn(true);

            assertThatThrownBy(() -> taxClassService.create(postVm))
                .isInstanceOf(DuplicatedException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void whenValidUpdate_shouldUpdateSuccessfully() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated VAT");
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
            when(taxClassRepository.existsByNameNotUpdatingTaxClass("Updated VAT", 1L)).thenReturn(false);

            taxClassService.update(postVm, 1L);

            assertThat(taxClass.getName()).isEqualTo("Updated VAT");
            verify(taxClassRepository).save(taxClass);
        }

        @Test
        void whenTaxClassNotFound_shouldThrowNotFoundException() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated");
            when(taxClassRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxClassService.update(postVm, 999L))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void whenNameConflictsWithOther_shouldThrowDuplicatedException() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Existing Name");
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
            when(taxClassRepository.existsByNameNotUpdatingTaxClass("Existing Name", 1L)).thenReturn(true);

            assertThatThrownBy(() -> taxClassService.update(postVm, 1L))
                .isInstanceOf(DuplicatedException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void whenTaxClassExists_shouldDeleteSuccessfully() {
            when(taxClassRepository.existsById(1L)).thenReturn(true);

            taxClassService.delete(1L);

            verify(taxClassRepository).deleteById(1L);
        }

        @Test
        void whenTaxClassNotFound_shouldThrowNotFoundException() {
            when(taxClassRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> taxClassService.delete(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetPageableTaxClasses {

        @Test
        void shouldReturnPaginatedResults() {
            TaxClass taxClass2 = TaxClass.builder().id(2L).name("GST").build();
            Page<TaxClass> page = new PageImpl<>(
                List.of(taxClass, taxClass2),
                PageRequest.of(0, 10),
                2
            );
            when(taxClassRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

            TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

            assertThat(result.taxClassContent()).hasSize(2);
            assertThat(result.pageNo()).isZero();
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        void whenEmpty_shouldReturnEmptyPage() {
            Page<TaxClass> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
            );
            when(taxClassRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

            TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

            assertThat(result.taxClassContent()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }
}
