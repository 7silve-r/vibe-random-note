package com.silver.account.controller;

import com.silver.account.dto.EmailDto;
import com.silver.account.service.EmailService;
import com.silver.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmailController {
    @Autowired private EmailService emailService;

    @PostMapping("/api/email/code")
    public Result<Void> send(@RequestBody EmailDto dto) {
        emailService.send(dto);
        return Result.success();
    }

    @PostMapping("/my/email")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> bind(@RequestBody EmailDto dto) {
        emailService.bind(dto);
        return Result.success();
    }

    @PostMapping("/api/password/reset")
    public Result<Void> reset(@RequestBody EmailDto dto) {
        emailService.reset(dto);
        return Result.success();
    }
}
