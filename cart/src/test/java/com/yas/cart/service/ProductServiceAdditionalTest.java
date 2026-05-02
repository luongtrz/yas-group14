package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.yas.cart.viewmodel.ProductThumbnailVm;
import com.yas.commonlibrary.config.ServiceUrlConfig;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class ProductServiceAdditionalTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;

    private static final String PRODUCT_URL = "http://api.yas.local/product";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() {
        restClient = Mockito.mock(RestClient.class);
        serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = Mockito.mock(RestClient.RequestHeadersSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);

        Mockito.when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(URI.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getProductById_whenProductExists_returnsFirstProduct() {
        ProductThumbnailVm product = new ProductThumbnailVm(1L, "Product 1", "product-1", "img.jpg");
        doReturn(ResponseEntity.ok(List.of(product)))
                .when(responseSpec).toEntity(Mockito.any(ParameterizedTypeReference.class));

        ProductThumbnailVm result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getProductById_whenProductListEmpty_returnsNull() {
        doReturn(ResponseEntity.ok(Collections.emptyList()))
                .when(responseSpec).toEntity(Mockito.any(ParameterizedTypeReference.class));

        ProductThumbnailVm result = productService.getProductById(99L);

        assertThat(result).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void existsById_whenProductExists_returnsTrue() {
        ProductThumbnailVm product = new ProductThumbnailVm(1L, "Product 1", "product-1", "img.jpg");
        doReturn(ResponseEntity.ok(List.of(product)))
                .when(responseSpec).toEntity(Mockito.any(ParameterizedTypeReference.class));

        boolean result = productService.existsById(1L);

        assertThat(result).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void existsById_whenProductNotFound_returnsFalse() {
        doReturn(ResponseEntity.ok(Collections.emptyList()))
                .when(responseSpec).toEntity(Mockito.any(ParameterizedTypeReference.class));

        boolean result = productService.existsById(999L);

        assertThat(result).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getProducts_whenEmptyIdList_returnsEmptyList() {
        doReturn(ResponseEntity.ok(Collections.emptyList()))
                .when(responseSpec).toEntity(Mockito.any(ParameterizedTypeReference.class));

        List<ProductThumbnailVm> result = productService.getProducts(Collections.emptyList());

        assertThat(result).isEmpty();
    }
}
