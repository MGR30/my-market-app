package ru.yandex.practicum.mymarket.support;


import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.PaymentService;

@SpringBootTest
public abstract class AbstractIntegrationTest extends TestcontainersBase {

    @MockitoBean
    protected PaymentService paymentService;

    protected void stubPaymentSuccess(long userId) {
        Mockito.when(paymentService.getBalance(userId)).thenReturn(Mono.just(1_000_000L));
        Mockito.when(paymentService.getBalance(Mockito.anyLong())).thenReturn(Mono.just(1_000_000L));
        Mockito.when(paymentService.pay(Mockito.anyLong(), Mockito.anyLong())).thenReturn(Mono.just(true));
    }

    protected void stubPaymentSuccess() {
        Mockito.when(paymentService.getBalance(Mockito.anyLong())).thenReturn(Mono.just(1_000_000L));
        Mockito.when(paymentService.pay(Mockito.anyLong(), Mockito.anyLong())).thenReturn(Mono.just(true));
    }
}
