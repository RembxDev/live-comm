package com.s26984.chatservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "chat.stomp.auth")
public class StompAuthProperties {
    private boolean enabled = true;

}
