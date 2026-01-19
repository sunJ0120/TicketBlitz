package com.example.be.queue.util;

public final class QueueRedisKey {

  private static final String QUEUE_PREFIX = "queue:concert:";
  private static final String ACTIVE_PREFIX = "active:concert:";
  private static final String TOKEN_PREFIX = "token:concert:";
  private static final String META_PREFIX = "meta:concert:";
  private static final String OPEN_CONCERTS = "open:concerts";
  private static final String HEARTBEAT_PREFIX = "heartbeat:concert:";

  private QueueRedisKey() {}

  public static String queue(Long concertId) {
    return QUEUE_PREFIX + concertId;
  }

  public static String active(Long concertId) {
    return ACTIVE_PREFIX + concertId;
  }

  public static String token(Long concertId, Long userId) {
    return TOKEN_PREFIX + concertId + ":user:" + userId;
  }

  public static String meta(Long concertId) {
    return META_PREFIX + concertId;
  }

  public static String openConcerts() {
    return OPEN_CONCERTS;
  }

  public static String heartbeat(Long concertId, Long userId) {
    return HEARTBEAT_PREFIX + concertId + ":user:" + userId;
  }
}
