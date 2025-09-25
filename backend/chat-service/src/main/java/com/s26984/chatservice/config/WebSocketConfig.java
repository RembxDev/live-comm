package com.s26984.chatservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@RequiredArgsConstructor
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${chat.stomp.allowed-origins:*}") private String[] allowedOrigins;
    @Value("${chat.stomp.relay.enabled:true}") private boolean relayEnabled;
    @Value("${chat.stomp.relay.host:rabbitmq}") private String relayHost;
    @Value("${chat.stomp.relay.port:61613}")    private int relayPort;
    @Value("${chat.stomp.relay.system-login:chat}")  private String systemLogin;
    @Value("${chat.stomp.relay.system-passcode:chat}") private String systemPasscode;
    @Value("${chat.stomp.relay.client-login:chat}")  private String clientLogin;
    @Value("${chat.stomp.relay.client-passcode:chat}") private String clientPasscode;

    private final StompAuthChannelInterceptor authInterceptor;



    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (relayEnabled) {
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setSystemLogin(systemLogin)
                    .setSystemPasscode(systemPasscode)
                    .setClientLogin(clientLogin)
                    .setClientPasscode(clientPasscode)
                    .setSystemHeartbeatSendInterval(10000)
                    .setSystemHeartbeatReceiveInterval(10000);
        } else {
            config.enableSimpleBroker("/topic", "/queue");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration reg) {
        reg.taskExecutor().corePoolSize(4).maxPoolSize(16).queueCapacity(1000);
        reg.interceptors(authInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration reg) {
        reg.taskExecutor().corePoolSize(4).maxPoolSize(16).queueCapacity(1000);
    }
}
