package com.s26984.chatservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StompAuthProperties.class})
public class PropsConfig {}
