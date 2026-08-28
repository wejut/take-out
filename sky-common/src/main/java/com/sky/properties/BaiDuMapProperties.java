package com.sky.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
@ConfigurationProperties(prefix = "cnm.baidu")
public class BaiDuMapProperties {
    private String address;
    private String ak;
}
