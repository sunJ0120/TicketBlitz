package com.example.be.auth.controller;

import com.example.be.auth.dto.LoginRequest;
import com.example.be.auth.dto.LoginResponse;
import com.example.be.auth.dto.SignupRequest;
import com.example.be.auth.service.AuthService;
import com.example.be.auth.util.AuthHttpHelper;
import com.example.be.auth.validator.AuthValidator;
import com.example.be.security.jwt.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthValidator authValidator;
    private final AuthHttpHelper authHttpHelper;
    private final JwtUtils jwtUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 성공");
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 또는 비밀번호")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        // accesss token만 전달
        return buildLoginResponse(response);
    }

    // 소셜 로그인 구현
    @Operation(summary = "소셜 로그인", description = "Oauth2 프로토콜을 활용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "403", description = "잘못된 접근")
    })
    @GetMapping("/login/social")
    public void socialLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // TODO: 계정 연동 정책 개선 필요 (GitHub Issue #XX)
        // - 다른 이메일의 소셜 계정으로 중복 유저 생성 가능
        // - 일반 로그인 유저 소셜 연동 시 확인 절차 없음

        // forward 검증
        if (request.getAttribute("OAUTH2_AUTHENTICATED") == null) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        String provider = (String) request.getAttribute("provider");
        String providerId = (String) request.getAttribute("providerId");
        String email = (String) request.getAttribute("email");
        String name = (String) request.getAttribute("name");

        LoginResponse loginResponse = authService.socialLogin(provider, providerId, email, name);

        // refreshToken 분리해서 따로 HttpOnly에 저장
        String refreshToken = loginResponse.refreshToken();
        ResponseCookie responseCookie = authHttpHelper.createRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        // accesss token만 전달
        response.sendRedirect(frontendUrl + "/oauth/callback?token=" + loginResponse.accessToken());
    }

    @Operation(
            summary = "로그아웃",
            description = "Access Token을 블랙리스트에 등록하여 로그아웃 처리합니다.",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = jwtUtils.resolveToken(request);

        try {
            token = authValidator.validateAndGetToken(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logout(token);
        ResponseCookie cookie = authHttpHelper.createLogoutCookie();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String refreshToken = authHttpHelper.extractRefreshToken(request);

        try {
            refreshToken = authValidator.validateAndGetToken(refreshToken);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponse response;

        try {
            response = authService.refresh(refreshToken);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return buildLoginResponse(response);
    }

    private ResponseEntity<LoginResponse> buildLoginResponse(LoginResponse response) {
        ResponseCookie cookie = authHttpHelper.createRefreshTokenCookie(response.refreshToken());
        LoginResponse body = new LoginResponse(response.accessToken(), null);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }
}
