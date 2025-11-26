package com.gogila.dashboard.infrastructure.adapter.persistence;

import com.gogila.dashboard.domain.screen.ScreenSchema;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * @Author Roy @DataTime 2025/11/26 11:50 @Mail roooyhe@163.com
 */
public interface ScreenSchemaRepository extends ReactiveMongoRepository<ScreenSchema, String> {

  Mono<ScreenSchema> findByCode(String code);
}