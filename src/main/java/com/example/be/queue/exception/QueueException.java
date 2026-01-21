package com.example.be.queue.exception;

import com.example.be.common.exception.BusinessException;

public class QueueException extends BusinessException {
  public QueueException(QueueErrorCode errorCode) {
    super(errorCode);
  }
}
