package com.example.be.auth.service;

import com.example.be.auth.dto.LoginRequest;
import com.example.be.auth.dto.LoginResponse;
import com.example.be.auth.dto.SignupRequest;
import com.example.be.security.jwt.JwtProvider;
import com.example.be.user.domain.SocialAccount;
import com.example.be.user.domain.User;
import com.example.be.user.enums.Provider;
import com.example.be.user.enums.Role;
import com.example.be.user.repository.SocialAccountRepository;
import com.example.be.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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
      throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
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
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
    }

    return createTokens(user);
  }

  @Transactional
  public LoginResponse socialLogin(String provider, String providerId, String email, String name) {
    Provider providerEnum = Provider.valueOf(provider.toUpperCase());
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

    SocialAccount newSocialAccount =
        createSocialAccount(providerEnum, providerId, existingUser.get());
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
      throw new BadCredentialsException("Refresh Token이 유효하지 않거나 탈취되었습니다."); // 서버 측 검증 실패 시 구체적인 예외
    }

    String newAccessToken = jwtProvider.generateAccessToken(userId, role);
    String newRefreshToken = jwtProvider.generateRefreshToken(userId, role); // 새로 생성

    long newExpirationMillis =
        jwtProvider.getExpiration(newRefreshToken) - System.currentTimeMillis();
    redisTokenService.addToWhitelist(userId, newRefreshToken, newExpirationMillis); // 화이트리스트 갱신

    return new LoginResponse(newAccessToken, newRefreshToken);
  }
}
