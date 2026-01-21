package com.example.be.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthHttpHelper {

  private static final String REFRESH_TOKEN_NAME = "refresh_token";
  private static final String REFRESH_REQUEST_URL = "/"; // 서비스 전역에서 쿠키를 인식할 수 있도록 설정
  private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 60 * 60;

  public String extractRefreshToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (REFRESH_TOKEN_NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return buildCookieBase(refreshToken, REFRESH_TOKEN_MAX_AGE_SECONDS).build();
  }

  public ResponseCookie createLogoutCookie() {
    return buildCookieBase("", 0).build();
  }

  private ResponseCookie.ResponseCookieBuilder buildCookieBase(String value, int maxAge) {
    return ResponseCookie.from(REFRESH_TOKEN_NAME, value)
        .maxAge(maxAge)
        .path(REFRESH_REQUEST_URL)
        .secure(true)
        .httpOnly(true)
        .sameSite("Lax");
  }
}
