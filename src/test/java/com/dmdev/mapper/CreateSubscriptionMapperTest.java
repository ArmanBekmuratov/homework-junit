package com.dmdev.mapper;

import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSubscriptionMapperTest {

    private final CreateSubscriptionMapper mapper = CreateSubscriptionMapper.getInstance();

    @Test
    void map() {
        Instant fixed = Instant.now();
        CreateSubscriptionDto dto = CreateSubscriptionDto.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE.name())
                .expirationDate(fixed)
                .build();

        Subscription actualResult = mapper.map(dto);

        Subscription expectedResult = Subscription.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE)
                .status(Status.ACTIVE)
                .expirationDate(fixed)
                .build();
        assertThat(actualResult).isEqualTo(expectedResult);
    }
}