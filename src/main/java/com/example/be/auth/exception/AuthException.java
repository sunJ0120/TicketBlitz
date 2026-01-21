package com.example.be.auth.exception;

import com.example.be.common.exception.BusinessException;
import lombok.Getter;

@Getter
public class AuthException extends BusinessException {
  public AuthException(AuthErrorCode errorCode) {
    super(errorCode);
  }
}
