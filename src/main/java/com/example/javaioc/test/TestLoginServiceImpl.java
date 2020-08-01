package com.example.javaioc.test;

import com.example.javaioc.myannotation.MyService;
@MyService
public class TestLoginServiceImpl implements LoginService {
    @Override
    public String login()
    {
        return "测试多态依赖注入";
    }
}