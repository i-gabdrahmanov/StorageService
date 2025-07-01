package com.storage.storageservice.controller;

import com.storage.storageservice.config.AppParamsConfig;
import com.storage.storageservice.service.ZkConfigWatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v2/zk")
@RequiredArgsConstructor
@DependsOn("zkConfigWatcher")
public class ZkController {

    private final ZkConfigWatcher zkConfigWatcher;

    @GetMapping
    public String getConfig() {
        AppParamsConfig appConfig = zkConfigWatcher.getAppParamsConfig();
        System.out.println(appConfig);
        return appConfig.getTestParam();
    }
}
