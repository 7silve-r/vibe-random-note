package com.silver.account.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.entity.User;
import com.silver.account.mapper.UserMapper;
import com.silver.account.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}
