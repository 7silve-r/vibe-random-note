package com.silver.mail.service;

public interface MailService {
    void send(String recipient, String subject, String text);
}
