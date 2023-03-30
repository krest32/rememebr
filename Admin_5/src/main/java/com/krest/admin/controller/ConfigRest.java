package com.krest.admin.controller;

import com.krest.admin.config.CoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("config")
public class ConfigRest {

    @Autowired
    CoreConfig config;

    @GetMapping("get")
    public CoreConfig getConfig() {
        return config;
    }
}
