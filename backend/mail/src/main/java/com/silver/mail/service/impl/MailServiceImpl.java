package com.silver.mail.service.impl;

import com.silver.common.exception.BusinessException;
import com.silver.mail.service.MailService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {
    private final ObjectProvider<JavaMailSender> mail;
    private final String sender;

    public MailServiceImpl(
            ObjectProvider<JavaMailSender> mail, @Value("${spring.mail.username:}") String sender) {
        this.mail = mail;
        this.sender = sender;
    }

    @Override
    public void send(String recipient, String subject, String text) {
        JavaMailSender client = mail.getIfAvailable();
        if (client == null || sender.isBlank()) throw new BusinessException(503, "邮件服务未配置");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        try {
            client.send(message);
        } catch (MailException ex) {
            throw new IllegalStateException("邮件发送失败", ex);
        }
    }
}
