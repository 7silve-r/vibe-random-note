package com.silver.account.utils;

import com.silver.common.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public record Account(Integer id, String username) {}

    public static Integer userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Account account)) {
            throw new BusinessException(401, "请先登录再操作");
        }
        return account.id();
    }

    public static Long optionalUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Account account
                ? account.id().longValue()
                : null;
    }

    public static boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream()
                        .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
    }
}
