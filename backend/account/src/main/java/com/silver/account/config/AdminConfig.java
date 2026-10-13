package com.silver.account.config;

import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminConfig {
    @Bean
    public ApplicationRunner admin(
            UserService users,
            PasswordEncoder encoder,
            @Value("${admin.password:}") String password) {
        return args -> {
            User admin = users.lambdaQuery().eq(User::getUsername, "ADMIN").one();
            if (admin != null) {
                if (!"ADMIN".equals(admin.getRole()))
                    throw new IllegalStateException("ADMIN 账号角色不正确");
                return;
            }
            if (password.length() < 8 || password.length() > 64) {
                throw new IllegalStateException("首次启动须设置8到64位的 ADMIN_PASSWORD");
            }
            admin = new User();
            admin.setUsername("ADMIN");
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setPassword(encoder.encode(password));
            if (!users.save(admin)) throw new IllegalStateException("管理员初始化失败");
        };
    }
}
