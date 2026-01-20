package com.example.be.websocket.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode {
  MISSING_AUTH_HEADER("WS_001", "Authorization 헤더가 없습니다"),
  INVALID_TOKEN_FORMAT("WS_002", "토큰 형식이 올바르지 않습니다"),
  EXPIRED_TOKEN("WS_003", "만료된 토큰입니다"),
  INVALID_TOKEN("WS_004", "유효하지 않은 토큰입니다");

  private final String code;
  private final String message;
}
