package com.example.be.websocket.interceptor;

import com.example.be.security.jwt.JwtProvider;
import com.example.be.websocket.dto.StompPrincipal;
import com.example.be.websocket.exception.WebSocketAuthException;
import com.example.be.websocket.exception.WebSocketErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    // CONNECT 명령일 때만 인증 처리 >> 만약 메시지 타입이 CONNECT라면, 헤더에서 토큰을 꺼내 유효성을 검사한다.
    if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

    if (authHeader == null) {
      throw new WebSocketAuthException(WebSocketErrorCode.MISSING_AUTH_HEADER);
    }

    if (!authHeader.startsWith(AUTHORIZATION_HEADER_PREFIX)) {
      throw new WebSocketAuthException(WebSocketErrorCode.INVALID_TOKEN_FORMAT);
    }

    String token = authHeader.substring(AUTHORIZATION_HEADER_PREFIX.length());

    if (!jwtProvider.validateToken(token)) {
      throw new WebSocketAuthException(WebSocketErrorCode.INVALID_TOKEN);
    }

    Long userId = jwtProvider.getUserId(token);
    accessor.setUser(new StompPrincipal(userId)); // WebSocket 세션에 사용자 정보 저장
    log.info("WebSocket 연결 성공 - userId: {}", userId);

    return message;
  }
}
