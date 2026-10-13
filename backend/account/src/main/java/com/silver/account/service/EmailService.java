package com.silver.account.service;

import com.silver.account.dto.EmailDto;

public interface EmailService {
    void send(EmailDto dto);

    void bind(EmailDto dto);

    void reset(EmailDto dto);
}
