package com.silver.mail.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.common.exception.BusinessException;
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
}
