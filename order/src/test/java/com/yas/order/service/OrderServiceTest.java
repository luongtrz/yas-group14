package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    private static final Long ORDER_ID = 1L;
    private static final String CHECKOUT_ID = "checkout-abc-123";

    private Order buildOrder(Long id, OrderStatus status) {
        OrderAddress address = OrderAddress.builder().id(1L).contactName("Test").phone("000").build();
        return Order.builder()
                .id(id)
                .orderStatus(status)
                .paymentStatus(PaymentStatus.PENDING)
                .checkoutId(CHECKOUT_ID)
                .shippingAddressId(address)
                .billingAddressId(address)
                .build();
    }

    @Nested
    class GetOrderWithItemsById {
        @Test
        void getOrderWithItemsById_whenOrderExists_returnsOrderVm() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderItemRepository.findAllByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            OrderVm result = orderService.getOrderWithItemsById(ORDER_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(ORDER_ID);
        }

        @Test
        void getOrderWithItemsById_whenOrderNotFound_throwsNotFoundException() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(ORDER_ID));
        }
    }

    @Nested
    class GetLatestOrders {
        @Test
        void getLatestOrders_whenCountIsZero_returnsEmptyList() {
            List result = orderService.getLatestOrders(0);
            assertThat(result).isEmpty();
        }

        @Test
        void getLatestOrders_whenCountIsNegative_returnsEmptyList() {
            List result = orderService.getLatestOrders(-1);
            assertThat(result).isEmpty();
        }

        @Test
        void getLatestOrders_whenNoOrders_returnsEmptyList() {
            when(orderRepository.getLatestOrders(any())).thenReturn(Collections.emptyList());

            List result = orderService.getLatestOrders(5);

            assertThat(result).isEmpty();
        }

        @Test
        void getLatestOrders_whenHasOrders_returnsMappedList() {
            Order order = buildOrder(ORDER_ID, OrderStatus.ACCEPTED);
            when(orderRepository.getLatestOrders(any())).thenReturn(List.of(order));

            List result = orderService.getLatestOrders(5);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class FindOrderByCheckoutId {
        @Test
        void findOrderByCheckoutId_whenExists_returnsOrder() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            when(orderRepository.findByCheckoutId(CHECKOUT_ID)).thenReturn(Optional.of(order));

            Order result = orderService.findOrderByCheckoutId(CHECKOUT_ID);

            assertThat(result.getId()).isEqualTo(ORDER_ID);
        }

        @Test
        void findOrderByCheckoutId_whenNotFound_throwsNotFoundException() {
            when(orderRepository.findByCheckoutId(CHECKOUT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId(CHECKOUT_ID));
        }
    }

    @Nested
    class RejectOrder {
        @Test
        void rejectOrder_whenOrderExists_setsStatusToReject() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderService.rejectOrder(ORDER_ID, "Out of stock");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
            assertThat(order.getRejectReason()).isEqualTo("Out of stock");
        }

        @Test
        void rejectOrder_whenOrderNotFound_throwsNotFoundException() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.rejectOrder(ORDER_ID, "reason"));
        }
    }

    @Nested
    class AcceptOrder {
        @Test
        void acceptOrder_whenOrderExists_setsStatusToAccepted() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderService.acceptOrder(ORDER_ID);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        void acceptOrder_whenOrderNotFound_throwsNotFoundException() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.acceptOrder(ORDER_ID));
        }
    }

    @Nested
    class UpdateOrderPaymentStatus {
        @Test
        void updateOrderPaymentStatus_whenCompleted_setsOrderStatusToPaid() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            PaymentOrderStatusVm statusVm = PaymentOrderStatusVm.builder()
                    .orderId(ORDER_ID)
                    .paymentId(1L)
                    .paymentStatus(PaymentStatus.COMPLETED.name())
                    .build();

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(statusVm);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.paymentId()).isEqualTo(1L);
        }

        @Test
        void updateOrderPaymentStatus_whenNotCompleted_doesNotChangeToPaid() {
            Order order = buildOrder(ORDER_ID, OrderStatus.PENDING);
            PaymentOrderStatusVm statusVm = PaymentOrderStatusVm.builder()
                    .orderId(ORDER_ID)
                    .paymentId(2L)
                    .paymentStatus(PaymentStatus.PENDING.name())
                    .build();

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderService.updateOrderPaymentStatus(statusVm);

            assertThat(order.getOrderStatus()).isNotEqualTo(OrderStatus.PAID);
        }

        @Test
        void updateOrderPaymentStatus_whenOrderNotFound_throwsNotFoundException() {
            PaymentOrderStatusVm statusVm = PaymentOrderStatusVm.builder()
                    .orderId(ORDER_ID)
                    .paymentStatus(PaymentStatus.PENDING.name())
                    .build();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(statusVm));
        }
    }
}
