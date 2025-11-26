package com.gogila.dashboard.interfaces.rest;

import com.gogila.dashboard.application.service.ScreenService;
import com.gogila.dashboard.domain.screen.ScreenSchema;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @Author Roy @DataTime 2025/11/26 11:52 @Mail roooyhe@163.com
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScreenController {

  private final ScreenService screenService;

  /** 通过 code 获取大屏 schema */
  @GetMapping("/screens/{code}")
  public Mono<ScreenSchema> getScreen(@PathVariable String code) {
    return screenService.getByCode(code);
  }

  /** 保存/更新 schema（简单示例，后面可以拆 DTO） */
  @PostMapping("/screens")
  public Mono<ScreenSchema> saveScreen(@RequestBody ScreenSchema schema) {
    return screenService.save(schema);
  }

  /** 示例数据接口：返回当前在线人数（随机数） GET /api/data/online-users */
  @GetMapping("/data/online-users")
  public Mono<Map<String, Object>> getOnlineUsers() {
    // 实际应查询数据库或缓存，这里简单返回随机数
    int value = (int) (Math.random() * 1000);
    return Mono.delay(Duration.ofMillis(50)).map(ignore -> Map.of("value", value));
  }
}