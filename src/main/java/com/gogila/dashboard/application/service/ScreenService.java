package com.gogila.dashboard.application.service;

import com.gogila.dashboard.domain.screen.ScreenSchema;
import com.gogila.dashboard.infrastructure.adapter.persistence.ScreenSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @Author Roy @DataTime 2025/11/26 11:52 @Mail roooyhe@163.com
 */
@Service
@RequiredArgsConstructor
public class ScreenService {

  private final ScreenSchemaRepository repository;

  public Mono<ScreenSchema> getByCode(String code) {
    return repository.findByCode(code);
  }

  public Mono<ScreenSchema> save(ScreenSchema schema) {
    return repository.save(schema);
  }
}