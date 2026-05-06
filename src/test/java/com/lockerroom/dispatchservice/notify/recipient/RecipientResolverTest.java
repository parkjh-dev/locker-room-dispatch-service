package com.lockerroom.dispatchservice.notify.recipient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lockerroom.dispatchservice.infrastructure.client.ResourceServiceClient;
import com.lockerroom.dispatchservice.infrastructure.client.dto.RecipientContactResponse;
import com.lockerroom.dispatchservice.infrastructure.exceptions.CustomException;
import com.lockerroom.dispatchservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.dispatchservice.log.DispatchChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipientResolverTest {

    private ResourceServiceClient client;
    private Cache<Long, Recipient> cache;
    private RecipientResolver resolver;

    @BeforeEach
    void setUp() {
        client = mock(ResourceServiceClient.class);
        cache = Caffeine.newBuilder().maximumSize(100).build();
        resolver = new RecipientResolver(client, cache);
    }

    @Test
    void resolve_callsExternalAndCachesResult() {
        when(client.getRecipientContact(1L)).thenReturn(new RecipientContactResponse(
                1L, "a@b.c", "01012345678", List.of("MAIL", "SMS"), true));

        Optional<Recipient> first = resolver.resolve(1L);
        Optional<Recipient> second = resolver.resolve(1L);

        assertThat(first).isPresent();
        assertThat(first.get().email()).isEqualTo("a@b.c");
        assertThat(first.get().preferredChannels()).containsExactly(DispatchChannel.MAIL, DispatchChannel.SMS);
        assertThat(first.get().optIn()).isTrue();
        assertThat(second).isPresent();
        verify(client, times(1)).getRecipientContact(1L);
    }

    @Test
    void resolve_unknownChannelString_isFiltered() {
        when(client.getRecipientContact(2L)).thenReturn(new RecipientContactResponse(
                2L, "a@b.c", "01012345678", List.of("MAIL", "FAX", "SMS"), true));

        Optional<Recipient> result = resolver.resolve(2L);

        assertThat(result.get().preferredChannels())
                .containsExactly(DispatchChannel.MAIL, DispatchChannel.SMS);
    }

    @Test
    void resolve_optOutFalse_returnsRecipientWithOptInFalse() {
        when(client.getRecipientContact(3L)).thenReturn(new RecipientContactResponse(
                3L, "a@b.c", "01012345678", List.of("MAIL"), false));

        Optional<Recipient> result = resolver.resolve(3L);

        assertThat(result).isPresent();
        assertThat(result.get().isOptOut()).isTrue();
    }

    @Test
    void resolve_externalThrows_returnsEmpty() {
        when(client.getRecipientContact(4L))
                .thenThrow(new CustomException(ErrorCode.RESOURCE_SERVICE_CALL_FAILED));

        Optional<Recipient> result = resolver.resolve(4L);

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_nullUserId_returnsEmpty() {
        Optional<Recipient> result = resolver.resolve(null);

        assertThat(result).isEmpty();
    }

    @Test
    void invalidate_clearsCacheEntry() {
        when(client.getRecipientContact(5L)).thenReturn(new RecipientContactResponse(
                5L, "a@b.c", "01012345678", List.of("MAIL"), true));

        resolver.resolve(5L);
        resolver.invalidate(5L);
        resolver.resolve(5L);

        verify(client, times(2)).getRecipientContact(5L);
    }
}
