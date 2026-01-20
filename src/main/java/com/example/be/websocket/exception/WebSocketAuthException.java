package com.example.be.websocket.exception;

public class WebSocketAuthException extends RuntimeException {
  private final WebSocketErrorCode errorCode;

  public WebSocketAuthException(WebSocketErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
