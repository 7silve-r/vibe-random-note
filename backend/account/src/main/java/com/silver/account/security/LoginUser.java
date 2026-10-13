package com.silver.account.security;

import com.silver.account.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.AuthorityUtils;

@Getter
public class LoginUser extends org.springframework.security.core.userdetails.User {
    private final Integer userId;
    private final Integer tokenVersion;

    public LoginUser(User user) {
        super(
                user.getUsername(),
                user.getPassword(),
                Integer.valueOf(0).equals(user.getStatus()),
                true,
                true,
                true,
                AuthorityUtils.createAuthorityList(
                        "ADMIN".equals(user.getRole())
                                ? new String[] {"ROLE_USER", "ROLE_ADMIN"}
                                : new String[] {"ROLE_USER"}));
        this.userId = user.getId();
        this.tokenVersion = user.getTokenVersion();
    }
}
