package edu.unibague.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> { forwarded.set(ex); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        String responseId = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        assertThat(responseId).isNotBlank();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo(responseId);
    }

    @Test
    void keepsExistingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me").header(CorrelationIdFilter.HEADER_NAME, "abc-123"));
        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("abc-123");
    }
}
