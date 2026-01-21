package com.example.be.security.jwt;

import com.example.be.auth.exception.AuthErrorCode;
import com.example.be.auth.exception.AuthException;
import com.example.be.auth.service.RedisTokenService;
import com.example.be.auth.validator.AuthValidator;
import com.example.be.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final AuthValidator authValidator;
  private final JwtProvider jwtProvider;
  private final RedisTokenService redisTokenService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // @formatter:off
    String token = jwtProvider.resolveToken(request);

    // 토큰이 없으면 검증하지 않는다.
    if (token == null || token.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      authValidator.validateAccessToken(token);

      if (redisTokenService.isBlacklisted(token)) {
        handleException(response, AuthErrorCode.TOKEN_EXPIRED);
        return;
      }

      Authentication authentication = jwtProvider.getAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (AuthException e) {
      handleException(response, (AuthErrorCode) e.getErrorCode());
      return;
    }
    filterChain.doFilter(request, response);
  }

  // exception 응답 처리를 위한 method
  private void handleException(HttpServletResponse response, AuthErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType("application/json;charset=UTF-8");

    // ErrorResponse.of(errorCode) 객체를 JSON으로 변환
    String jsonResponse = new ObjectMapper().writeValueAsString(ErrorResponse.of(errorCode));

    response.getWriter().println(jsonResponse);
  }
}
