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

    public String validateRefreshToken(String token) {
        try {
            String type = jwtProvider.getTokenType(token);

            if (!type.equals("REFRESH")) {
                throw new AuthException(AuthErrorCode.TOKEN_INVALID);
            }
            return token;
        } catch (JwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    public String validateAccessToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }

        try {
            String type = jwtProvider.getTokenType(token);

            if (!type.equals("ACCESS")) {
                throw new AuthException(AuthErrorCode.TOKEN_INVALID);
            }
            return token;
        } catch (JwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }
}
