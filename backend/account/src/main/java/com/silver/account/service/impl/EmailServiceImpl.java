package com.silver.account.service.impl;

import com.silver.account.dto.EmailDto;
import com.silver.account.entity.*;
import com.silver.account.mapper.EmailCodeMapper;
import com.silver.account.service.*;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.exception.BusinessException;
import com.silver.mail.service.MailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired private EmailCodeMapper codeMapper;
    @Autowired private UserService userService;
    @Autowired private MailService mail;
    @Autowired private PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    private String email(EmailDto dto) {
        String email = dto.getEmail();
        if (email == null
                || email.length() > 254
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new BusinessException("邮箱格式不正确");
        }
        return email.toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    @Transactional
    public void send(EmailDto dto) {
        String email = email(dto);
        String purpose = dto.getPurpose();
        if (!"bind".equals(purpose) && !"reset".equals(purpose))
            throw new BusinessException("验证码用途不正确");
        if ("bind".equals(purpose)) SecurityUtil.userId();
        EmailCode saved = codeMapper.lock(email, purpose);
        LocalDateTime now = LocalDateTime.now();
        if (saved != null && saved.getSentAt().plusSeconds(60).isAfter(now))
            throw new BusinessException(429, "请60秒后再试");
        String code = String.format("%06d", random.nextInt(1000000));
        EmailCode record = saved == null ? new EmailCode() : saved;
        record.setEmail(email);
        record.setPurpose(purpose);
        record.setCode(encoder.encode(code));
        record.setSentAt(now);
        record.setExpiresAt(now.plusMinutes(5));
        record.setAttempts(0);
        if (saved == null) codeMapper.insert(record);
        else codeMapper.updateById(record);
        mail.sendVerificationCode(email, code, purpose);
    }

    private void verify(EmailDto dto, String purpose) {
        EmailCode saved = codeMapper.lock(email(dto), purpose);
        if (saved == null
                || saved.getExpiresAt().isBefore(LocalDateTime.now())
                || saved.getAttempts() >= 5) {
            throw new BusinessException("验证码无效或已过期");
        }
        if (dto.getCode() == null || !encoder.matches(dto.getCode(), saved.getCode())) {
            saved.setAttempts(saved.getAttempts() + 1);
            codeMapper.updateById(saved);
            throw new BusinessException("验证码错误");
        }
        codeMapper.deleteById(saved.getId());
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void bind(EmailDto dto) {
        Integer id = SecurityUtil.userId();
        String email = email(dto);
        if (userService.lambdaQuery().eq(User::getEmail, email).ne(User::getId, id).exists())
            throw new BusinessException("邮箱已被使用");
        verify(dto, "bind");
        if (!userService
                .lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getEmail, email)
                .set(User::getEmailVerified, true)
                .update()) {
            throw new BusinessException("邮箱绑定失败");
        }
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void reset(EmailDto dto) {
        if (dto.getNewPwd() == null
                || dto.getNewPwd().length() < 8
                || dto.getNewPwd().length() > 64
                || !dto.getNewPwd().equals(dto.getReNewPwd()))
            throw new BusinessException("两次密码须一致且为8到64位");
        verify(dto, "reset");
        User user =
                userService
                        .lambdaQuery()
                        .eq(User::getEmail, email(dto))
                        .eq(User::getEmailVerified, true)
                        .one();
        if (user == null || "ADMIN".equals(user.getRole()))
            throw new BusinessException("该账号不能通过邮箱重置密码");
        if (!userService
                .lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPassword, encoder.encode(dto.getNewPwd()))
                .setSql("token_version = token_version + 1")
                .update()) throw new BusinessException("密码重置失败");
    }
}
