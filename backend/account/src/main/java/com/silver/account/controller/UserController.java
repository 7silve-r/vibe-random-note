package com.silver.account.controller;

import com.silver.account.dto.LoginDto;
import com.silver.account.dto.PasswordDto;
import com.silver.account.dto.ProfileDto;
import com.silver.account.dto.RegisterDto;
import com.silver.account.entity.User;
import com.silver.account.security.LoginUser;
import com.silver.account.service.UserService;
import com.silver.account.upload.AccountUploadService;
import com.silver.account.utils.JwtUtil;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import com.silver.storage.model.UploadResult;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private AuthenticationManager authenticationManager;

    @Autowired private UserService userService;

    @Autowired private JwtUtil jwtUtil;

    @Autowired private AccountUploadService uploadService;

    /** 注册 */
    @PostMapping("/api/reg")
    public Result<Void> register(@RequestBody RegisterDto dto) {
        if (dto.getUsername() == null
                || !dto.getUsername().matches("[a-zA-Z0-9_]{5,30}")
                || dto.getPassword() == null
                || dto.getPassword().length() < 8
                || dto.getPassword().length() > 64)
            throw new BusinessException("用户名需5到30位字母/数字/下划线，密码需8到64位");
        if (userService.lambdaQuery().eq(User::getUsername, dto.getUsername()).exists()) {
            throw new BusinessException("用户名已存在");
        }
        if (!dto.getPassword().equals(dto.getRePassword())) {
            throw new BusinessException("两次密码不一致");
        }
        if ("ADMIN".equalsIgnoreCase(dto.getUsername())) throw new BusinessException("该用户名不可注册");
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (!userService.save(user)) {
            throw new BusinessException("操作未完成，数据可能已变化，请刷新后重试");
        }
        return Result.success();
    }

    /** 登录 */
    @PostMapping("/api/login")
    public Result<String> login(@RequestBody LoginDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank() || dto.getPassword() == null) {
            throw new BusinessException("用户名或密码错误");
        }
        LoginUser user;
        try {
            var authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated(
                                    dto.getUsername(), dto.getPassword()));
            user = (LoginUser) authentication.getPrincipal();
        } catch (DisabledException ex) {
            throw new BusinessException(403, "账号已停用");
        } catch (BadCredentialsException ex) {
            throw new BusinessException("用户名或密码错误");
        }
        return Result.success(jwtUtil.generateToken(user.getUsername(), user.getTokenVersion()));
    }

    /** 获取个人信息 */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my/profile")
    public Result<User> getUserInfo() {
        User user = currentUser();
        user.setPassword(null);
        return Result.success(user);
    }

    /** 更新资料 */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/my/profile")
    public Result<Void> updateProfile(@RequestBody ProfileDto dto) {
        User user = currentUser();
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setEmailVerified(false);
        if (!userService.updateById(user)) {
            throw new BusinessException("操作未完成，数据可能以变化，请刷新后重试");
        }
        return Result.success();
    }

    /** 更新密码 */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/my/password")
    public Result<Void> updatePassword(@RequestBody PasswordDto dto) {
        User user = currentUser();
        if (dto.getOldPwd() == null
                || !passwordEncoder.matches(dto.getOldPwd(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (dto.getNewPwd() == null
                || dto.getNewPwd().length() < 8
                || dto.getNewPwd().length() > 64) {
            throw new BusinessException("新密码需8到64位");
        }
        if (!dto.getNewPwd().equals(dto.getReNewPwd())) {
            throw new BusinessException("两次密码不一致");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPwd()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        if (!userService.updateById(user)) {
            throw new BusinessException("操作未完成，数据可能已变化，请刷新后重试");
        }
        return Result.success();
    }

    /** 更新头像 */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping(value = "/my/avatar", consumes = "multipart/form-data")
    public Result<UploadResult> updateAvatar(@RequestParam("file") MultipartFile file)
            throws IOException {
        return Result.success(uploadService.avatar(SecurityUtil.userId(), file));
    }

    @PostMapping("/my/logout")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> logout() {
        if (!userService
                .lambdaUpdate()
                .eq(User::getId, SecurityUtil.userId())
                .setSql("token_version = token_version + 1")
                .update()) {
            throw new BusinessException("退出失败");
        }
        return Result.success();
    }

    @DeleteMapping("/my/account")
    @PreAuthorize("hasRole('USER') and !hasRole('ADMIN')")
    public Result<Void> delete() {
        if (!userService.removeById(SecurityUtil.userId())) throw new BusinessException("账号注销失败");
        return Result.success();
    }

    private User currentUser() {
        User user = userService.getById(SecurityUtil.userId());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        return user;
    }
}
