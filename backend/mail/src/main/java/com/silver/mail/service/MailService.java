package com.silver.mail.service;

public interface MailService {
    void send(String recipient, String subject, String text);

    void sendVerificationCode(String recipient, String code, String purpose);
}
