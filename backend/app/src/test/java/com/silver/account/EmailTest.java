package com.silver.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.account.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "spring.mail.username=test@example.com")
@ActiveProfiles("test")
@Transactional
class EmailTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @MockitoBean JavaMailSender mail;
    MockMvc mvc;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM email_code");
        jdbc.update("DELETE FROM `user`");
        jdbc.update(
                "INSERT INTO `user` (id, username, email, email_verified, role) VALUES (1, 'writer01', 'reader@example.com', TRUE, 'USER'), (2, 'ADMIN', 'admin@example.com', TRUE, 'ADMIN')");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    void code(String email, String purpose) {
        jdbc.update(
                "INSERT INTO email_code (email, purpose, code, expires_at, sent_at, attempts) VALUES (?, ?, ?, DATEADD('MINUTE', 5, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, 0)",
                email,
                purpose,
                encoder.encode("123456"));
    }

    String body(String email, String code) {
        return "{\"email\":\""
                + email
                + "\",\"code\":\""
                + code
                + "\",\"newPwd\":\"NewPass123!\",\"reNewPwd\":\"NewPass123!\"}";
    }

    @Test
    void send() throws Exception {
        mvc.perform(
                        post("/api/email/code")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"reader@example.com\",\"purpose\":\"reset\"}"))
                .andExpect(status().isOk());
        var sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(sent.capture());
        assertArrayEquals(new String[] {"reader@example.com"}, sent.getValue().getTo());
        String value = sent.getValue().getText().replaceAll("[^0-9]", "").substring(0, 6);
        assertTrue(
                encoder.matches(
                        value, jdbc.queryForObject("SELECT code FROM email_code", String.class)));
    }

    @Test
    void throttle() throws Exception {
        code("reader@example.com", "reset");
        mvc.perform(
                        post("/api/email/code")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"reader@example.com\",\"purpose\":\"reset\"}"))
                .andExpect(status().isTooManyRequests());
        verifyNoInteractions(mail);
    }

    @Test
    void reset() throws Exception {
        code("reader@example.com", "reset");
        mvc.perform(
                        post("/api/password/reset")
                                .contentType("application/json")
                                .content(body("reader@example.com", "123456")))
                .andExpect(status().isOk());
        assertTrue(
                encoder.matches(
                        "NewPass123!",
                        jdbc.queryForObject(
                                "SELECT password FROM `user` WHERE id = 1", String.class)));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT token_version FROM `user` WHERE id = 1", Integer.class));
        mvc.perform(
                        post("/api/password/reset")
                                .contentType("application/json")
                                .content(body("reader@example.com", "123456")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attempts() throws Exception {
        code("reader@example.com", "reset");
        for (int i = 0; i < 5; i++)
            mvc.perform(
                            post("/api/password/reset")
                                    .contentType("application/json")
                                    .content(body("reader@example.com", "000000")))
                    .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/password/reset")
                                .contentType("application/json")
                                .content(body("reader@example.com", "123456")))
                .andExpect(status().isBadRequest());
        assertEquals(5, jdbc.queryForObject("SELECT attempts FROM email_code", Integer.class));
    }

    @Test
    void bind() throws Exception {
        code("new@example.com", "bind");
        mvc.perform(
                        post("/my/email")
                                .header("Authorization", jwt.generateToken("writer01"))
                                .contentType("application/json")
                                .content(body("new@example.com", "123456")))
                .andExpect(status().isOk());
        assertEquals(
                "new@example.com",
                jdbc.queryForObject("SELECT email FROM `user` WHERE id = 1", String.class));
        assertTrue(
                jdbc.queryForObject(
                        "SELECT email_verified FROM `user` WHERE id = 1", Boolean.class));
    }

    @Test
    void admin() throws Exception {
        code("admin@example.com", "reset");
        mvc.perform(
                        post("/api/password/reset")
                                .contentType("application/json")
                                .content(body("admin@example.com", "123456")))
                .andExpect(status().isBadRequest());
    }
}
