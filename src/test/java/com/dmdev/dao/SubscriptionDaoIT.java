package com.dmdev.dao;

import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SubscriptionDaoIT extends IntegrationTestBase {

    private final SubscriptionDao subscriptionDao = SubscriptionDao.getInstance();

    @Test
    void findAll() {
        Subscription subscription1 = subscriptionDao.insert(getSubscription("test1"));
        Subscription subscription2 = subscriptionDao.insert(getSubscription("test2"));
        Subscription subscription3 = subscriptionDao.insert(getSubscription("test3"));

        List<Subscription> actualResult = subscriptionDao.findAll();

        assertThat(actualResult).hasSize(3);
        List<Integer> subscriptionIds = actualResult.stream()
                .map(Subscription::getId)
                .toList();
        assertThat(subscriptionIds).contains(subscription1.getId(), subscription2.getId(), subscription3.getId());
    }

    @Test
    void findById() {
        Subscription subscription = subscriptionDao.insert(getSubscription("test2"));

        Optional<Subscription> actualResult = subscriptionDao.findById(subscription.getId());

        assertThat(actualResult).isPresent();
        assertThat(actualResult.get()).isEqualTo(subscription);
    }

    @Test
    void insert() {
        Subscription subscription = getSubscription("Test");

        Subscription actualResult = subscriptionDao.insert(subscription);

        assertNotNull(actualResult.getId());
    }

    @Test
    void deleteExistingEntity() {
        Subscription subscription = subscriptionDao.insert(getSubscription("test"));

        boolean actualResult = subscriptionDao.delete(subscription.getId());

        assertTrue(actualResult);
    }

    @Test
    void deleteNonExistingEntity() {
        subscriptionDao.insert(getSubscription("test"));

        boolean actualResult = subscriptionDao.delete(100000);

        assertFalse(actualResult);
    }

    @Test
    void update() {
        Subscription subscription = subscriptionDao.insert(getSubscription("test"));
        subscription.setName("test updated");
        subscription.setStatus(Status.EXPIRED);

        subscriptionDao.update(subscription);

        Subscription updatedSubscription  = subscriptionDao.findById(subscription.getId()).get();
        assertThat(updatedSubscription).isEqualTo(subscription);
    }

    @Test
    void findByUserId() {
        Subscription subscription = subscriptionDao.insert(getSubscription("test"));

        List<Subscription> actualResult = subscriptionDao.findByUserId(subscription.getUserId());

        assertThat(actualResult).hasSize(1);
        assertThat(actualResult).contains(subscription);
    }

    @Test
    void shouldNotFindByUserIdIfSubscriptionDoesntExist() {
        subscriptionDao.insert(getSubscription("test"));

        List<Subscription> actualResult = subscriptionDao.findByUserId(1);

        assertThat(actualResult).isEmpty();
    }

    private Subscription getSubscription(String name) {
        return Subscription.builder()
                .userId(100)
                .name(name)
                .provider(Provider.APPLE)
                .status(Status.ACTIVE)
                .expirationDate(Instant.now())
                .build();
    }
}