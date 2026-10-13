package com.silver.account.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.silver.account.config.AuthenticationConfig;
import com.silver.account.dto.*;
import com.silver.account.entity.User;
import com.silver.account.security.LoginUser;
import com.silver.account.service.UserService;
import com.silver.account.support.TestData;
import com.silver.account.utils.JwtUtil;
import com.silver.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** 使用真实控制器，只模拟数据库和 JWT 依赖。 */
class UserControllerTest {
    UserService users;
    JwtUtil jwt;
    UserController controller;
    AuthenticationManager authenticationManager;
    LambdaQueryChainWrapper<User> query;
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        TestData.authenticate(1);
        users = mock(UserService.class);
        jwt = mock(JwtUtil.class);
        query = TestData.users(users);
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", users);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwt);
        var security = new AuthenticationConfig();
        authenticationManager =
                security.authenticationManager(security.userDetails(users), encoder);
        ReflectionTestUtils.setField(controller, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(controller, "authenticationManager", authenticationManager);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    RegisterDto reg() {
        var dto = new RegisterDto();
        dto.setUsername("writer01");
        dto.setPassword("OldPass123!");
        dto.setRePassword("OldPass123!");
        return dto;
    }

    LoginDto login() {
        var dto = new LoginDto();
        dto.setUsername("writer01");
        dto.setPassword("OldPass123!");
        return dto;
    }

    PasswordDto pwd() {
        var dto = new PasswordDto();
        dto.setOldPwd("OldPass123!");
        dto.setNewPwd("NewPass123!");
        dto.setReNewPwd("NewPass123!");
        return dto;
    }

    User account() {
        var user = TestData.user();
        user.setPassword(encoder.encode("OldPass123!"));
        when(query.one()).thenReturn(user);
        when(users.getById(1)).thenReturn(user);
        return user;
    }

    @Test
    void register() {
        when(users.save(any(User.class))).thenReturn(true);
        assertEquals(200, controller.register(reg()).getCode());
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertTrue(encoder.matches("OldPass123!", saved.getValue().getPassword()));
        assertNotEquals("OldPass123!", saved.getValue().getPassword());
    }

    @Test
    void dupUser() {
        when(query.exists()).thenReturn(true);
        assertThrows(BusinessException.class, () -> controller.register(reg()));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void badName() {
        var dto = reg();
        dto.setUsername("a");
        assertThrows(BusinessException.class, () -> controller.register(dto));
        verifyNoInteractions(users);
    }

    @Test
    void mismatch() {
        var dto = reg();
        dto.setRePassword("different");
        assertThrows(BusinessException.class, () -> controller.register(dto));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void saveFail() {
        when(users.save(any(User.class))).thenReturn(false);
        assertThrows(BusinessException.class, () -> controller.register(reg()));
    }

    @Test
    void loginOk() {
        account();
        when(jwt.generateToken("writer01", 0)).thenReturn("signed-token");
        assertEquals("signed-token", controller.login(login()).getData());
        verify(query).one();
    }

    @Test
    void loginVersion() {
        var user = account();
        user.setTokenVersion(7);
        var dto = login();
        dto.setUsername("WRITER01");
        when(jwt.generateToken("writer01", 7)).thenReturn("versioned-token");
        assertEquals("versioned-token", controller.login(dto).getData());
        verify(query).one();
    }

    @Test
    void loginPrincipal() {
        var user = account();
        user.setRole("ADMIN");
        user.setTokenVersion(7);
        var authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "writer01", "OldPass123!"));
        var principal = (LoginUser) authentication.getPrincipal();
        assertTrue(authentication.isAuthenticated());
        assertEquals(user.getId(), principal.getUserId());
        assertEquals(7, principal.getTokenVersion());
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertNull(principal.getPassword());
        assertNull(authentication.getCredentials());
        assertNotNull(user.getPassword());
    }

    @Test
    void disabledLogin() {
        account().setStatus(1);
        var error = assertThrows(BusinessException.class, () -> controller.login(login()));
        assertEquals(403, error.getCode());
        assertEquals("账号已停用", error.getMessage());
        verifyNoInteractions(jwt);
    }

    @Test
    void missingCredentials() {
        var dto = login();
        dto.setPassword(null);
        assertThrows(BusinessException.class, () -> controller.login(dto));
        dto.setPassword("OldPass123!");
        dto.setUsername(null);
        assertThrows(BusinessException.class, () -> controller.login(dto));
        dto.setUsername(" ");
        assertThrows(BusinessException.class, () -> controller.login(dto));
        verifyNoInteractions(users, jwt);
    }

    @Test
    void badPwd() {
        account();
        var dto = login();
        dto.setPassword("wrong");
        assertThrows(BusinessException.class, () -> controller.login(dto));
        verifyNoInteractions(jwt);
    }

    @Test
    void noUser() {
        assertThrows(BusinessException.class, () -> controller.login(login()));
        verifyNoInteractions(jwt);
    }

    @Test
    void hidePwd() {
        account();
        assertNull(controller.getUserInfo().getData().getPassword());
        verify(users).getById(1);
        verify(query, never()).one();
        verifyNoInteractions(jwt);
    }

    @Test
    void goneUser() {
        assertEquals(
                401,
                assertThrows(ResponseStatusException.class, () -> controller.getUserInfo())
                        .getStatusCode()
                        .value());
    }

    @Test
    void oldPwd() {
        account();
        var dto = pwd();
        dto.setOldPwd("wrong");
        assertThrows(BusinessException.class, () -> controller.updatePassword(dto));
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改密码后应保存 BCrypt 散列，而非明文")
    void hashPwd() {
        account();
        when(users.updateById(any(User.class))).thenReturn(true);
        controller.updatePassword(pwd());
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).updateById(saved.capture());
        assertNotEquals("NewPass123!", saved.getValue().getPassword(), "修改密码不能保存明文");
        assertTrue(encoder.matches("NewPass123!", saved.getValue().getPassword()));
    }

    @Test
    @DisplayName("确认密码缺失应返回业务异常，不能抛空指针")
    void noRepeat() {
        account();
        var dto = pwd();
        dto.setReNewPwd(null);
        assertThrows(BusinessException.class, () -> controller.updatePassword(dto));
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void noIdentity() {
        SecurityContextHolder.clearContext();
        assertEquals(
                401,
                assertThrows(BusinessException.class, () -> controller.getUserInfo()).getCode());
        verifyNoInteractions(users, jwt);
    }

    @Test
    void profileOwner() {
        var user = TestData.user();
        user.setId(2);
        when(users.getById(2)).thenReturn(user);
        when(users.updateById(any(User.class))).thenReturn(true);
        TestData.authenticate(2);
        var dto = new ProfileDto();
        dto.setNickname("新昵称");
        controller.updateProfile(dto);
        verify(users).getById(2);
        verify(users).updateById(user);
        assertEquals("新昵称", user.getNickname());
        verifyNoInteractions(jwt);
    }

    @Test
    void avatarOwner() throws Exception {
        var uploads = mock(com.silver.account.upload.AccountUploadService.class);
        ReflectionTestUtils.setField(controller, "uploadService", uploads);
        var file = TestData.png();
        TestData.authenticate(2);
        controller.updateAvatar(file);
        verify(uploads).avatar(2, file);
        verifyNoInteractions(users, jwt);
    }
}
