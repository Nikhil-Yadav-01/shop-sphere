package com.rudraksha.shopsphere.analytics.service;

import com.rudraksha.shopsphere.analytics.document.AnalyticsEvent;
import com.rudraksha.shopsphere.analytics.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private AnalyticsEventRepository repository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void ingestEvent_Success() {
        analyticsService.ingestEvent("PROD_VIEW", "user-123", "sess-456", new HashMap<>(), "127.0.0.1", "Chrome");

        verify(repository).save(any(AnalyticsEvent.class));
    }

    @Test
    void getTotalEventCount_Success() {
        analyticsService.getTotalEventCount();
        verify(repository).count();
    }
}
