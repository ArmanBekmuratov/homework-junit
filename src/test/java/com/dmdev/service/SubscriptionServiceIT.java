package com.dmdev.service;

import com.dmdev.dao.SubscriptionDao;
import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.exception.SubscriptionException;
import com.dmdev.integration.IntegrationTestBase;
import com.dmdev.mapper.CreateSubscriptionMapper;
import com.dmdev.validator.CreateSubscriptionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionServiceIT extends IntegrationTestBase {

    private SubscriptionService subscriptionService;
    private SubscriptionDao subscriptionDao;
    private Clock clock;

    @BeforeEach
    void init() {
        subscriptionService = new SubscriptionService(
                subscriptionDao = SubscriptionDao.getInstance(),
                CreateSubscriptionMapper.getInstance(),
                CreateSubscriptionValidator.getInstance(),
                clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
        );
    }

    @Test
    void upsert() {
        CreateSubscriptionDto createSubscriptionDto = getCreateSubscriptionDto();

        Subscription actualResult = subscriptionService.upsert(createSubscriptionDto);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getName()).isEqualTo(createSubscriptionDto.getName());
        assertThat(actualResult.getProvider().name()).isEqualTo(createSubscriptionDto.getProvider());
        assertThat(actualResult.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void shouldCancelActiveSubscription() {
        Subscription subscription = subscriptionDao.insert(getSubscription());

        subscriptionService.cancel(subscription.getId());
        Optional<Subscription> canceledSubscription = subscriptionDao.findById(subscription.getId());

        assertThat(canceledSubscription).isPresent();
        assertThat(canceledSubscription.get().getStatus()).isEqualTo(Status.CANCELED);
    }

    @Test
    void shouldThrowExceptionCancelNotActiveSubscription() {
        Subscription subscription = subscriptionDao.insert(getSubscription().setStatus(Status.CANCELED));

        assertThrows(SubscriptionException.class,
                () -> subscriptionService.cancel(subscription.getId()));
    }

    @Test
    void shouldExpireActiveSubscription() {
        Subscription subscription = subscriptionDao.insert(getSubscription());
        subscriptionService.expire(subscription.getId());
        Optional<Subscription> updatedSubscription = subscriptionDao.findById(subscription.getId());

        assertThat(updatedSubscription).isPresent();
        assertThat(updatedSubscription.get().getStatus()).isEqualTo(Status.EXPIRED);

        assertThat(updatedSubscription.get().getExpirationDate()).isEqualTo(clock.instant());
    }

    private CreateSubscriptionDto getCreateSubscriptionDto() {
        return CreateSubscriptionDto.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE.name())
                .expirationDate(Instant.now(clock).plusSeconds(1000))
                .build();
    }

    private Subscription getSubscription() {
        return Subscription.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE)
                .status(Status.ACTIVE)
                .expirationDate(Instant.now(clock).plusSeconds(1000))
                .build();
    }

}