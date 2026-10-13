package com.silver.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "WEB_PORT=5173")
@ActiveProfiles("test")
class CorsTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void allowedPreflight() throws Exception {
        for (String host : new String[] {"localhost", "127.0.0.1"}) {
            String origin = "http://" + host + ":5173";
            mvc.perform(
                            options("/my/profile")
                                    .header("Origin", origin)
                                    .header("Access-Control-Request-Method", "PATCH")
                                    .header(
                                            "Access-Control-Request-Headers",
                                            "Authorization,Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
    }

    @Test
    void rejectedOrigin() throws Exception {
        mvc.perform(
                        options("/my/profile")
                                .header("Origin", "https://untrusted.example")
                                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void authenticationStillRequired() throws Exception {
        mvc.perform(get("/my/profile").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
