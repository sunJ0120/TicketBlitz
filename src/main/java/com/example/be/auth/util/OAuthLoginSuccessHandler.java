package com.example.be.auth.util;

import com.example.be.auth.dto.OAuthUserInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
    String provider = authToken.getAuthorizedClientRegistrationId(); // "kakao", "naver", "google"

    OAuth2User oAuth2User = authToken.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    OAuthUserInfo userInfo = extractUserInfo(provider, attributes);

    request.setAttribute("provider", userInfo.provider());
    request.setAttribute("providerId", userInfo.providerId());
    request.setAttribute("email", userInfo.email());
    request.setAttribute("name", userInfo.name());
    request.setAttribute("OAUTH2_AUTHENTICATED", true);

    request.getRequestDispatcher("/api/v1/auth/login/social").forward(request, response);
  }

  private OAuthUserInfo extractUserInfo(String provider, Map<String, Object> attributes) {
    return switch (provider) {
      case "kakao" -> extractKakao(attributes);
      case "naver" -> extractNaver(attributes);
      case "google" -> extractGoogle(attributes);
      default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
    };
  }

  private OAuthUserInfo extractKakao(Map<String, Object> attributes) {
    String providerId = attributes.get("id").toString();

    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    String email = (String) kakaoAccount.get("email");

    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
    String name = (String) profile.get("nickname");

    return new OAuthUserInfo("kakao", providerId, email, name);
  }

  private OAuthUserInfo extractNaver(Map<String, Object> attributes) {
    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
    return new OAuthUserInfo(
        "naver",
        (String) response.get("id"),
        (String) response.get("email"),
        (String) response.get("name"));
  }

  private OAuthUserInfo extractGoogle(Map<String, Object> attributes) {
    return new OAuthUserInfo(
        "google",
        (String) attributes.get("sub"),
        (String) attributes.get("email"),
        (String) attributes.get("name"));
  }
}
