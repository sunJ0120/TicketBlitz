package com.example.be.auth.service;

import com.example.be.auth.dto.LoginRequest;
import com.example.be.auth.dto.LoginResponse;
import com.example.be.auth.dto.SignupRequest;
import com.example.be.auth.exception.AuthErrorCode;
import com.example.be.auth.exception.AuthException;
import com.example.be.security.jwt.JwtProvider;
import com.example.be.user.domain.SocialAccount;
import com.example.be.user.domain.User;
import com.example.be.user.enums.Provider;
import com.example.be.user.enums.Role;
import com.example.be.user.repository.SocialAccountRepository;
import com.example.be.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

  private final SocialAccountRepository socialAccountRepository;
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final PasswordEncoder passwordEncoder;
  private final RedisTokenService redisTokenService;

  @Transactional
  public void signup(SignupRequest request) {
    if (userRepository.findByEmail(request.email()).isPresent()) {
      throw new AuthException(AuthErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    String encodedPassword = passwordEncoder.encode(request.password());

    User user =
        User.builder()
            .email(request.email())
            .password(encodedPassword)
            .name(request.name())
            .role(Role.USER)
            .build();

    userRepository.save(user);
  }

  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_USER_NOT_FOUND));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new AuthException(AuthErrorCode.AUTH_WRONG_PASSWORD);
    }

    return createTokens(user);
  }

  @Transactional
  public LoginResponse socialLogin(String provider, String providerId, String email, String name) {
    Provider providerEnum;
    try {
      providerEnum = Provider.valueOf(provider.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new AuthException(AuthErrorCode.AUTH_BAD_REQUEST);
    }
    // 1. 이미 해당 provider로 연동된 소셜 아이디가 있을 경우
    Optional<SocialAccount> existingSocial =
        socialAccountRepository.findByProviderAndProviderId(providerEnum, providerId);

    if (existingSocial.isPresent()) {
      return createTokens(existingSocial.get().getUser());
    }

    // 2. 유저가 있을 경우
    Optional<User> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent()) {
      SocialAccount newSocialAccount =
          createSocialAccount(providerEnum, providerId, existingUser.get());
      socialAccountRepository.save(newSocialAccount);

      return createTokens(existingUser.get());
    }

    // 3. 유저도 없고. 소셜도 없을 경우
    User newUser = User.builder().email(email).password(null).name(name).role(Role.USER).build();
    userRepository.save(newUser);

    SocialAccount newSocialAccount = createSocialAccount(providerEnum, providerId, newUser);
    socialAccountRepository.save(newSocialAccount);

    return createTokens(newUser);
  }

  private SocialAccount createSocialAccount(Provider providerEnum, String providerId, User user) {
    return SocialAccount.builder().provider(providerEnum).providerId(providerId).user(user).build();
  }

  private LoginResponse createTokens(User user) {
    String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
    String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

    long expirationMillis = jwtProvider.getExpiration(refreshToken) - System.currentTimeMillis();
    redisTokenService.addToWhitelist(user.getId(), refreshToken, expirationMillis);

    return new LoginResponse(accessToken, refreshToken);
  }

  public void logout(String token) {
    long expirationMillis = jwtProvider.getExpiration(token) - System.currentTimeMillis();
    redisTokenService.addToBlacklist(token, expirationMillis);

    Long userId = jwtProvider.getUserId(token);
    redisTokenService.deleteRefreshToken(userId);

    SecurityContextHolder.clearContext();
  }

  public LoginResponse refresh(String refreshToken) {
    Long userId = jwtProvider.getUserId(refreshToken);
    Role role = jwtProvider.getRole(refreshToken);

    if (!redisTokenService.isValidRefreshToken(userId, refreshToken)) { // Redis 서버측 검증
      throw new AuthException(AuthErrorCode.TOKEN_INVALID); // 서버 측 검증 실패 시 구체적인 예외
    }

    String newAccessToken = jwtProvider.generateAccessToken(userId, role);
    String newRefreshToken = jwtProvider.generateRefreshToken(userId, role); // 새로 생성

    long newExpirationMillis =
        jwtProvider.getExpiration(newRefreshToken) - System.currentTimeMillis();
    redisTokenService.addToWhitelist(userId, newRefreshToken, newExpirationMillis); // 화이트리스트 갱신

    return new LoginResponse(newAccessToken, newRefreshToken);
  }
}
