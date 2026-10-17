package com.silver.mail.service.impl;

import com.silver.common.exception.BusinessException;
import com.silver.mail.service.MailService;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {
    private final ObjectProvider<JavaMailSender> mail;
    private final String sender;
    private final String verificationTemplate;

    public MailServiceImpl(
            ObjectProvider<JavaMailSender> mail, @Value("${spring.mail.username:}") String sender) {
        this.mail = mail;
        this.sender = sender;
        try {
            this.verificationTemplate =
                    new ClassPathResource("mail/verification.html")
                            .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("邮件模板读取失败", ex);
        }
    }

    @Override
    public void sendVerificationCode(String recipient, String code, String purpose) {
        String action =
                switch (purpose) {
                    case "bind" -> "绑定邮箱";
                    case "reset" -> "重置密码";
                    default -> throw new IllegalArgumentException("验证码用途不正确");
                };
        if (code == null || !code.matches("[0-9]{6}"))
            throw new IllegalArgumentException("验证码必须为6位数字");
        JavaMailSender client = mail.getIfAvailable();
        if (client == null || sender.isBlank()) throw new BusinessException(503, "邮件服务未配置");
        String text =
                "你正在"
                        + action
                        + "。\n\n验证码："
                        + code
                        + "\n5分钟内有效，请回到刚才的页面输入。\n请勿向他人透露验证码。如非本人操作，请忽略此邮件。\n\nVIBE · 日记 / 音乐 / 诗词";
        String html = verificationTemplate.replace("{{action}}", action).replace("{{code}}", code);
        try {
            var message = client.createMimeMessage();
            var helper =
                    new MimeMessageHelper(
                            message,
                            MimeMessageHelper.MULTIPART_MODE_RELATED,
                            StandardCharsets.UTF_8.name());
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject("VIBE · " + action + "验证码");
            helper.setText(text, html);
            helper.addInline(
                    "vibe-sky", new ClassPathResource("mail/images/sky-braids.png"), "image/png");
            client.send(message);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("邮件发送失败", ex);
        }
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
