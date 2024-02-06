package com.dmdev.service;

import com.dmdev.dao.SubscriptionDao;
import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.exception.SubscriptionException;
import com.dmdev.exception.ValidationException;
import com.dmdev.mapper.CreateSubscriptionMapper;
import com.dmdev.validator.CreateSubscriptionValidator;
import com.dmdev.validator.Error;
import com.dmdev.validator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private CreateSubscriptionValidator createSubscriptionValidator;
    @Mock
    private SubscriptionDao subscriptionDao;
    @Mock
    private CreateSubscriptionMapper createSubscriptionMapper;
    @Mock
    private Clock clock;
    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void initClock() {
        lenient().when(clock.instant()).thenReturn(Instant.now());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void shouldUpdateExistingSubscriptionWhenUpsert () {
        CreateSubscriptionDto dto = getCreateSubscriptionDto();
        Subscription subscription = getSubscription();
        doReturn(new ValidationResult()).when(createSubscriptionValidator).validate(dto);
        doReturn(Collections.singletonList(subscription)).when(subscriptionDao).findByUserId(subscription.getUserId());
        doReturn(subscription).when(subscriptionDao).upsert(subscription);

        Subscription actualResult = subscriptionService.upsert(dto);

        verify(subscriptionDao, times(1)).upsert(subscription);
        assertThat(actualResult).isEqualTo(subscription);
    }

    @Test
    void shouldCreateNewSubscriptionWhenUpsert() {
        CreateSubscriptionDto dto = getCreateSubscriptionDto();
        Subscription subscription = getSubscription();
        doReturn(new ValidationResult()).when(createSubscriptionValidator).validate(dto);
        doReturn(Collections.emptyList()).when(subscriptionDao).findByUserId(subscription.getUserId());
        doReturn(subscription).when(createSubscriptionMapper).map(dto);
        doReturn(subscription).when(subscriptionDao).upsert(subscription);

        Subscription actualResult = subscriptionService.upsert(dto);

        verify(subscriptionDao, times(1)).upsert(subscription);
        assertThat(actualResult).isEqualTo(subscription);
    }

    @Test
    void shouldNotPassValidationWhenUpsert() {
        CreateSubscriptionDto dto = getCreateSubscriptionDto();
        ValidationResult validationResult = new ValidationResult();
        validationResult.getErrors().add(Error.of(100, "test"));
        doReturn(validationResult).when(createSubscriptionValidator).validate(dto);

        assertThrows(ValidationException.class, () -> subscriptionService.upsert(dto));

        verify(subscriptionDao, never()).findByUserId(anyInt());
        verify(subscriptionDao, never()).upsert(any());
        verify(createSubscriptionMapper, never()).map(any());
    }

    @Test
    void shouldCancelActiveSubscription() {
        Subscription subscription = getSubscription();
        when(subscriptionDao.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        subscriptionService.cancel(subscription.getId());

        assertThat(subscription.getStatus()).isEqualTo(Status.CANCELED);
        verify(subscriptionDao, times(1)).update(subscription);
    }

    @Test
    void shouldThrowExceptionWhenCancellingCanceledSubscription() {
        Subscription subscription = getSubscription();
        subscription.setStatus(Status.CANCELED);
        when(subscriptionDao.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThrows(SubscriptionException.class, () -> subscriptionService.cancel(subscription.getId()));

        assertThat(subscription.getStatus()).isEqualTo(Status.CANCELED);
        verify(subscriptionDao, never()).update(subscription);
    }

    @Test
    void shouldCancelNonExistentSubscription() {
        when(subscriptionDao.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> subscriptionService.cancel(1));

        verify(subscriptionDao, never()).update(any());
    }

    @Test
    void shouldExpireActiveSubscription() {
        Subscription subscription = getSubscription();
        doReturn(Optional.of(subscription)).when(subscriptionDao).findById(subscription.getId());
        doReturn(subscription).when(subscriptionDao).update(subscription);

        subscriptionService.expire(subscription.getId());

        assertThat(subscription.getStatus()).isEqualTo(Status.EXPIRED);
        assertThat(subscription.getExpirationDate()).isEqualTo(Instant.now(clock));
        verify(subscriptionDao, times(1)).update(subscription);
    }

    @Test
    void shouldThrowExceptionForAlreadyExpiredSubscription() {
        Subscription subscription = getSubscription();
        subscription.setStatus(Status.EXPIRED);
        when(subscriptionDao.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThrows(SubscriptionException.class,
                () -> subscriptionService.expire(subscription.getId()));

        assertThat(subscription.getStatus()).isEqualTo(Status.EXPIRED);
        assertThat(subscription.getExpirationDate()).isNotNull();
        verify(subscriptionDao, never()).update(subscription);
    }

    @Test
    void shouldThrowExceptionForNonExistentSubscription() {
        when(subscriptionDao.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> subscriptionService.cancel(999));
        verify(subscriptionDao, never()).update(any());
    }

    private CreateSubscriptionDto getCreateSubscriptionDto() {
        return CreateSubscriptionDto.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE.name())
                .expirationDate(Instant.now(clock))
                .build();
    }

    private Subscription getSubscription() {
        return Subscription.builder()
                .userId(100)
                .name("test")
                .provider(Provider.APPLE)
                .status(Status.ACTIVE)
                .expirationDate(Instant.now(clock))
                .build();
    }
}