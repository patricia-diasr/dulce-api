package com.dulce.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingLoginCodeNotifier implements LoginCodeNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingLoginCodeNotifier.class);

    @Override
    public void notifyLoginCode(String customerName, String email, String code) {
        log.info("[DEV] Código de login para {} <{}>: {}", customerName, email, code);
    }
}
