package com.example.be.queue.service;

import com.example.be.queue.dto.QueueStatusResponse;
import com.example.be.queue.exception.QueueErrorCode;
import com.example.be.queue.exception.QueueException;
import com.example.be.queue.util.QueueRedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

  private static final int PROCESS_RATE = 50; // 1회당 입장 인원
  private static final int SCHEDULER_INTERVAL = 3; // 스케줄러 주기 (초)

  private final RedisTemplate<String, Object> redisTemplate;

  public QueueStatusResponse enter(Long concertId, Long userId) {
    String activeKey = QueueRedisKey.active(concertId);
    Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, userId);

    if (Boolean.TRUE.equals(isActive)) {
      throw new QueueException(QueueErrorCode.ALREADY_ACTIVE);
    }

    String queueKey = QueueRedisKey.queue(concertId);
    Double existingScore = redisTemplate.opsForZSet().score(queueKey, userId);

    if (existingScore != null) {
      return getQueueStatusResponse(userId, queueKey);
    }

    double newScore = System.currentTimeMillis();
    redisTemplate.opsForZSet().add(queueKey, userId, newScore);
    redisTemplate.opsForSet().add(QueueRedisKey.openConcerts(), concertId);

    return getQueueStatusResponse(userId, queueKey);
  }

  public QueueStatusResponse getStatus(Long concertId, Long userId) {
    String activeKey = QueueRedisKey.active(concertId); // ACTIVE 상태 먼저 체크
    Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, userId);

    if (Boolean.TRUE.equals(isActive)) {
      return QueueStatusResponse.active(); // ACTIVE 상태 반환
    }

    String queueKey = QueueRedisKey.queue(concertId);
    Double score = redisTemplate.opsForZSet().score(queueKey, userId);

    if (score == null) {
      throw new QueueException(QueueErrorCode.NOT_IN_QUEUE);
    }

    return getQueueStatusResponse(userId, queueKey);
  }

  private QueueStatusResponse getQueueStatusResponse(Long userId, String queueKey) {
    Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
    if (rank == null) {
      throw new QueueException(QueueErrorCode.QUEUE_OPERATION_FAILED); // 예상치 못한 상황 방어코드
    }
    rank += 1;

    Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);
    Long estimatedWaitSeconds = calculateEstimatedTime(rank);

    return QueueStatusResponse.waiting(rank, totalWaiting, estimatedWaitSeconds);
  }

  private Long calculateEstimatedTime(Long rank) {
    long waitingAhead = rank;
    long cycles = (waitingAhead / PROCESS_RATE) + 1;
    return cycles * SCHEDULER_INTERVAL;
  }
}
