package com.example.be.auth.exception;

import com.example.be.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  // 회원 가입, 계정 (400 Bad Request)
  AUTH_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_001", "이미 존재하는 이메일입니다"),
  AUTH_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "AUTH_002", "이미 존재하는 닉네임입니다"),
  AUTH_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_003", "비밀번호 형식이 올바르지 않습니다"),

  // 로그인 및 인증 관련 (401 Unauthorized)
  AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_004", "존재하지 않는 사용자입니다"),
  AUTH_WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_005", "비밀번호가 일치하지 않습니다"),
  AUTH_LOCKED_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_006", "잠긴 계정입니다"),

  // 토큰 검증 및 만료
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_007", "만료된 Access Token입니다"),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_008", "유효하지 않은 토큰입니다"),
  TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "AUTH_009", "JWT 구조가 올바르지 않습니다"),

  // Refresh Token 관련
  REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_010", "세션이 만료되었습니다. 다시 로그인해주세요"),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_011", "쿠키에 Refresh Token이 없습니다"),

  // 권한 관련
  AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_012", "인증 정보가 없습니다"),
  AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_013", "접근 권한이 부족합니다"),

  AUTH_BAD_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_014", "잘못된 요청입니다");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
