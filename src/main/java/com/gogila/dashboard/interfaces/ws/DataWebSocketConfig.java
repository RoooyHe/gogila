package com.gogila.dashboard.interfaces.ws;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * @Author Roy @DataTime 2025/11/26 11:55 @Mail roooyhe@163.com
 */
@Configuration
@RequiredArgsConstructor
public class DataWebSocketConfig {

  private final DataWebSocketHandler dataWebSocketHandler;

  @Bean
  public SimpleUrlHandlerMapping webSocketMapping() {
    Map<String, Object> map = Map.of("/ws/data/online-users", dataWebSocketHandler);
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    mapping.setOrder(-1); // 优先级高于普通 Handler
    mapping.setUrlMap(map);
    return mapping;
  }

  @Bean
  public WebSocketHandlerAdapter handlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}