package com.gogila.dashboard.interfaces.ws;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @Author Roy @DataTime 2025/11/26 11:53 @Mail roooyhe@163.com
 */
@Slf4j
@Component
public class DataWebSocketHandler implements WebSocketHandler {

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    // 这里简单模拟一个实时指标数据流，你后面替换为真实数据即可
    Flux<WebSocketMessage> flux =
        Flux.interval(Duration.ofSeconds(1))
            .map(
                i -> {
                  int value = (int) (Math.random() * 1000);
                  Map<String, Object> payload =
                      Map.of("value", value, "timestamp", System.currentTimeMillis());
                  return session.textMessage(payload.toString());
                });

    return session.send(flux);
  }
}