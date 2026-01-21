package com.example.be.auth.validator;

import com.example.be.auth.exception.AuthErrorCode;
import com.example.be.auth.exception.AuthException;
import com.example.be.security.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthValidator {

  private final JwtProvider jwtProvider;

  public String validateAndGetToken(String token) throws JwtException {
    if (token == null || token.isBlank()) {
      throw new AuthException(AuthErrorCode.TOKEN_INVALID);
    }

    jwtProvider.validateToken(token);

    return token;
  }
}
