package com.silver.account.config;

import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.account.utils.JwtUtil;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final UserService users;
    private final HandlerExceptionResolver resolver;

    public JwtFilter(JwtUtil jwt, UserService users, HandlerExceptionResolver resolver) {
        this.jwt = jwt;
        this.users = users;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        try {
            if (token != null && !token.isBlank()) {
                String username = jwt.getUsername(token);
                User user = users.lambdaQuery().eq(User::getUsername, username).one();
                if (user == null
                        || !Integer.valueOf(0).equals(user.getStatus())
                        || !Objects.equals(user.getTokenVersion(), jwt.getVersion(token))) {
                    throw new BusinessException(401, "登录已失效，请重新登录");
                }
                var auth =
                        new UsernamePasswordAuthenticationToken(
                                new SecurityUtil.Account(user.getId(), user.getUsername()),
                                null,
                                AuthorityUtils.createAuthorityList(
                                        "ROLE_USER",
                                        "ADMIN".equals(user.getRole())
                                                ? "ROLE_ADMIN"
                                                : "ROLE_USER"));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            resolver.resolveException(
                    request, response, null, new BusinessException(401, "登录已失效，请重新登录"));
            return;
        } catch (Exception ex) {
            resolver.resolveException(request, response, null, ex);
            return;
        }
        chain.doFilter(request, response);
    }
}
