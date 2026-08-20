package com.dulce.backend.notification;

public interface LoginCodeNotifier {

    void notifyLoginCode(String customerName, String email, String code);
}
