package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserAddressService userAddressService;

    private static final String USER_ID = "user-123";
    private static final Long ADDRESS_ID = 10L;

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(USER_ID);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class GetUserAddressList {
        @Test
        void getUserAddressList_whenAnonymousUser_throwsAccessDeniedException() {
            Authentication auth = mock(Authentication.class);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn("anonymousUser");
            SecurityContextHolder.setContext(ctx);

            assertThrows(AccessDeniedException.class, () -> userAddressService.getUserAddressList());
        }

        @Test
        void getUserAddressList_whenNoAddresses_returnsEmptyList() {
            when(userAddressRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(locationService.getAddressesByIdList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ActiveAddressVm> result = userAddressService.getUserAddressList();

            assertThat(result).isEmpty();
        }

        @Test
        void getUserAddressList_whenHasAddresses_returnsMappedList() {
            UserAddress userAddress = UserAddress.builder()
                    .userId(USER_ID).addressId(ADDRESS_ID).isActive(true).build();
            AddressDetailVm addressDetail = new AddressDetailVm(ADDRESS_ID, "John", "0123456789",
                    "123 Main St", "City", "12345", 1L, "District", 1L, "Province", 1L, "Country");

            when(userAddressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(userAddress));
            when(locationService.getAddressesByIdList(List.of(ADDRESS_ID))).thenReturn(List.of(addressDetail));

            List<ActiveAddressVm> result = userAddressService.getUserAddressList();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }
    }

    @Nested
    class GetAddressDefault {
        @Test
        void getAddressDefault_whenAnonymousUser_throwsAccessDeniedException() {
            Authentication auth = mock(Authentication.class);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn("anonymousUser");
            SecurityContextHolder.setContext(ctx);

            assertThrows(AccessDeniedException.class, () -> userAddressService.getAddressDefault());
        }

        @Test
        void getAddressDefault_whenNoActiveAddress_throwsNotFoundException() {
            when(userAddressRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> userAddressService.getAddressDefault());
        }

        @Test
        void getAddressDefault_whenActiveAddressExists_returnsAddressDetail() {
            UserAddress userAddress = UserAddress.builder()
                    .userId(USER_ID).addressId(ADDRESS_ID).isActive(true).build();
            AddressDetailVm expected = new AddressDetailVm(ADDRESS_ID, "John", "0123456789",
                    "123 Main St", "City", "12345", 1L, "District", 1L, "Province", 1L, "Country");

            when(userAddressRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(userAddress));
            when(locationService.getAddressById(ADDRESS_ID)).thenReturn(expected);

            AddressDetailVm result = userAddressService.getAddressDefault();

            assertThat(result.id()).isEqualTo(ADDRESS_ID);
        }
    }

    @Nested
    class CreateAddress {
        @Test
        void createAddress_whenFirstAddress_setsAsActive() {
            AddressPostVm addressPostVm = mock(AddressPostVm.class);
            AddressVm addressVm = AddressVm.builder().id(ADDRESS_ID).build();
            UserAddress savedAddress = UserAddress.builder()
                    .userId(USER_ID).addressId(ADDRESS_ID).isActive(true).build();

            when(userAddressRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(locationService.createAddress(addressPostVm)).thenReturn(addressVm);
            when(userAddressRepository.save(any(UserAddress.class))).thenReturn(savedAddress);

            UserAddressVm result = userAddressService.createAddress(addressPostVm);

            assertThat(result).isNotNull();
        }

        @Test
        void createAddress_whenNotFirstAddress_setsAsInactive() {
            AddressPostVm addressPostVm = mock(AddressPostVm.class);
            AddressVm addressVm = AddressVm.builder().id(ADDRESS_ID).build();
            UserAddress existing = UserAddress.builder()
                    .userId(USER_ID).addressId(1L).isActive(true).build();
            UserAddress savedAddress = UserAddress.builder()
                    .userId(USER_ID).addressId(ADDRESS_ID).isActive(false).build();

            when(userAddressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(existing));
            when(locationService.createAddress(addressPostVm)).thenReturn(addressVm);
            when(userAddressRepository.save(any(UserAddress.class))).thenReturn(savedAddress);

            UserAddressVm result = userAddressService.createAddress(addressPostVm);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    class DeleteAddress {
        @Test
        void deleteAddress_whenAddressNotFound_throwsNotFoundException() {
            when(userAddressRepository.findOneByUserIdAndAddressId(USER_ID, ADDRESS_ID)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> userAddressService.deleteAddress(ADDRESS_ID));
        }

        @Test
        void deleteAddress_whenAddressExists_deletesSuccessfully() {
            UserAddress userAddress = UserAddress.builder()
                    .userId(USER_ID).addressId(ADDRESS_ID).isActive(false).build();
            when(userAddressRepository.findOneByUserIdAndAddressId(USER_ID, ADDRESS_ID)).thenReturn(userAddress);

            userAddressService.deleteAddress(ADDRESS_ID);

            verify(userAddressRepository).delete(userAddress);
        }
    }

    @Nested
    class ChooseDefaultAddress {
        @Test
        void chooseDefaultAddress_setsCorrectAddressAsActive() {
            Long otherId = 99L;
            UserAddress addr1 = UserAddress.builder().userId(USER_ID).addressId(ADDRESS_ID).isActive(false).build();
            UserAddress addr2 = UserAddress.builder().userId(USER_ID).addressId(otherId).isActive(true).build();

            when(userAddressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(addr1, addr2));

            userAddressService.chooseDefaultAddress(ADDRESS_ID);

            assertThat(addr1.getIsActive()).isTrue();
            assertThat(addr2.getIsActive()).isFalse();
            verify(userAddressRepository).saveAll(any());
        }
    }
}
