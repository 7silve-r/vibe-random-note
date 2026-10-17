package com.silver.mail.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.common.exception.BusinessException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceImplTest {
    final JavaMailSender sender = mock(JavaMailSender.class);

    MailServiceImpl service(String address, boolean configured) {
        var beans = new StaticListableBeanFactory();
        if (configured) beans.addBean("sender", sender);
        return new MailServiceImpl(beans.getBeanProvider(JavaMailSender.class), address);
    }

    @Test
    void send() {
        service("sender@example.com", true).send("reader@example.com", "主题", "内容");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("sender@example.com", message.getValue().getFrom());
        assertArrayEquals(new String[] {"reader@example.com"}, message.getValue().getTo());
        assertEquals("主题", message.getValue().getSubject());
        assertEquals("内容", message.getValue().getText());
    }

    @Test
    void missingConfiguration() {
        assertEquals(
                503,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service("sender@example.com", false)
                                                .send("reader@example.com", "主题", "内容"))
                        .getCode());
        assertEquals(
                503,
                assertThrows(
                                BusinessException.class,
                                () -> service("", true).send("reader@example.com", "主题", "内容"))
                        .getCode());
        verifyNoInteractions(sender);
    }

    @Test
    void deliveryFailure() {
        var failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(sender).send(any(SimpleMailMessage.class));
        var thrown =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service("sender@example.com", true)
                                        .send("reader@example.com", "主题", "内容"));
        assertSame(failure, thrown.getCause());
    }

    @Test
    void verificationContainsHtmlAndPlainText() throws Exception {
        for (String purpose : new String[] {"bind", "reset"}) {
            var message = new MimeMessage(Session.getInstance(new Properties()));
            when(sender.createMimeMessage()).thenReturn(message);
            service("sender@example.com", true)
                    .sendVerificationCode("reader@example.com", "012345", purpose);
            verify(sender).send(message);
            message.saveChanges();
            assertEquals("reader@example.com", message.getAllRecipients()[0].toString());
            assertEquals("sender@example.com", message.getFrom()[0].toString());
            String action = purpose.equals("bind") ? "绑定邮箱" : "重置密码";
            assertEquals("VIBE · " + action + "验证码", message.getSubject());
            var root = (Multipart) message.getContent();
            assertTrue(root.getContentType().startsWith("multipart/related"));
            assertEquals(2, root.getCount());
            var illustration = root.getBodyPart(1);
            assertTrue(illustration.isMimeType("image/png"));
            assertEquals("inline", illustration.getDisposition());
            assertEquals("<vibe-sky>", illustration.getHeader("Content-ID")[0]);
            try (var image = illustration.getInputStream()) {
                assertArrayEquals(
                        new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, image.readNBytes(8));
            }
            var alternative = (Multipart) root.getBodyPart(0).getContent();
            assertTrue(alternative.getContentType().startsWith("multipart/alternative"));
            assertEquals(2, alternative.getCount());
            assertTrue(alternative.getBodyPart(0).isMimeType("text/plain"));
            assertTrue(alternative.getBodyPart(1).isMimeType("text/html"));
            String plain = (String) alternative.getBodyPart(0).getContent();
            String html = (String) alternative.getBodyPart(1).getContent();
            assertTrue(plain.contains("验证码：012345"));
            assertTrue(plain.contains(action));
            assertTrue(html.contains("012345"));
            assertTrue(html.contains(action));
            assertTrue(html.contains("5 分钟内有效"));
            assertTrue(html.contains("src=\"cid:vibe-sky\""));
            assertFalse(html.contains("{{"));
        }
    }

    @Test
    void verificationDeliveryFailure() {
        when(sender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(sender).send(any(MimeMessage.class));
        var thrown =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service("sender@example.com", true)
                                        .sendVerificationCode(
                                                "reader@example.com", "123456", "reset"));
        assertSame(failure, thrown.getCause());
    }

    @Test
    void verificationRejectsInvalidContentAndMissingConfiguration() {
        var configured = service("sender@example.com", true);
        assertThrows(
                IllegalArgumentException.class,
                () -> configured.sendVerificationCode("reader@example.com", "<html>", "bind"));
        assertThrows(
                IllegalArgumentException.class,
                () -> configured.sendVerificationCode("reader@example.com", "123456", "unknown"));
        assertEquals(
                503,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service("", true)
                                                .sendVerificationCode(
                                                        "reader@example.com", "123456", "bind"))
                        .getCode());
        verifyNoInteractions(sender);
    }
}
