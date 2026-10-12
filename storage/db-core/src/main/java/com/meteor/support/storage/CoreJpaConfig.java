package com.meteor.support.storage;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * com.meteor 아래의 엔티티와 Spring Data 인터페이스(com.meteor.storage)를 스캔한다. 둘 다 이 모듈에 있고, 애그리거트와의
 * 변환은 core-api 의 domain.*Repository 어댑터가 한다.
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.meteor")
@EnableJpaRepositories(basePackages = "com.meteor")
class CoreJpaConfig {

}
