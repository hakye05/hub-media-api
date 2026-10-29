package com.hakye.mediahub.api.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanup {

    private final RefreshTokenService service;

    public RefreshTokenCleanup(RefreshTokenService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purge() {
        service.purgeExpired();
    }
}
