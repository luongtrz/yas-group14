package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yas.cart.viewmodel.ProductThumbnailVm;
import com.yas.commonlibrary.config.ServiceUrlConfig;
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
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    private RestClient.ResponseSpec responseSpec;

    private static final String PRODUCT_URL = "http://api.yas.local/product";

    @BeforeEach
    void setUp() {
        restClient = Mockito.mock(RestClient.class);
        serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(Mockito.any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getProductById_whenProductExists_returnsFirstProduct() {
        ProductThumbnailVm product = new ProductThumbnailVm(1L, "Product 1", "product-1", "img.jpg");
        when(responseSpec.toEntity(Mockito.<ParameterizedTypeReference<List<ProductThumbnailVm>>>any()))
                .thenReturn(ResponseEntity.ok(List.of(product)));

        ProductThumbnailVm result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getProductById_whenProductListEmpty_returnsNull() {
        when(responseSpec.toEntity(Mockito.<ParameterizedTypeReference<List<ProductThumbnailVm>>>any()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        ProductThumbnailVm result = productService.getProductById(99L);

        assertThat(result).isNull();
    }

    @Test
    void existsById_whenProductExists_returnsTrue() {
        ProductThumbnailVm product = new ProductThumbnailVm(1L, "Product 1", "product-1", "img.jpg");
        when(responseSpec.toEntity(Mockito.<ParameterizedTypeReference<List<ProductThumbnailVm>>>any()))
                .thenReturn(ResponseEntity.ok(List.of(product)));

        boolean result = productService.existsById(1L);

        assertThat(result).isTrue();
    }

    @Test
    void existsById_whenProductNotFound_returnsFalse() {
        when(responseSpec.toEntity(Mockito.<ParameterizedTypeReference<List<ProductThumbnailVm>>>any()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        boolean result = productService.existsById(999L);

        assertThat(result).isFalse();
    }

    @Test
    void getProducts_whenEmptyIdList_returnsEmptyList() {
        when(responseSpec.toEntity(Mockito.<ParameterizedTypeReference<List<ProductThumbnailVm>>>any()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        List<ProductThumbnailVm> result = productService.getProducts(Collections.emptyList());

        assertThat(result).isEmpty();
    }
}
