package com.silver.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc =
                MockMvcBuilders.standaloneSetup(new Endpoints())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void business() throws Exception {
        mvc.perform(get("/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("数据冲突"));
    }

    @Test
    void badJson() throws Exception {
        mvc.perform(post("/json").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void missingArg() throws Exception {
        mvc.perform(get("/number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void badType() throws Exception {
        mvc.perform(get("/number").param("id", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void noLogin() throws Exception {
        mvc.perform(get("/auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void badMethod() throws Exception {
        mvc.perform(post("/number"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void serverError() throws Exception {
        mvc.perform(get("/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("服务器内部错误，请稍后重试"));
    }

    @Test
    void database() throws Exception {
        mvc.perform(get("/database"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503));
    }

    @RestController
    static class Endpoints {
        @GetMapping("/business")
        public Result<Void> business() {
            throw new BusinessException(409, "数据冲突");
        }

        @PostMapping("/json")
        public Object json(@RequestBody java.util.Map<String, String> data) {
            return data;
        }

        @GetMapping("/number")
        public int number(@RequestParam("id") int id) {
            return id;
        }

        @GetMapping("/auth")
        public void auth() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        @GetMapping("/crash")
        public void crash() {
            throw new IllegalStateException("private database password");
        }

        @GetMapping("/database")
        public void database() {
            throw new org.springframework.dao.DataAccessResourceFailureException("private host");
        }
    }
}
