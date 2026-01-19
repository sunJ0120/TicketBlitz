package com.example.be.queue.scheduler;

import com.example.be.queue.util.QueueRedisKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final int MAX_ACTIVE_USERS = 100; // 한 번에 입장 가능한 인원수

  @Scheduled(fixedDelay = 3000) // 3초마다 실행한다.
  public void processQueue() {
    Set<Long> openConcertIds = getOpenConcertIds();

    for (Long concertId : openConcertIds) {
      processQueueForConcert(concertId);
    }
  }

  @Scheduled(fixedDelay = 10000) // 10초마다 실행한다.
  public void cleanInactiveConcerts() {
    Set<Long> openConcertIds = getOpenConcertIds();
    for (Long concertId : openConcertIds) {
      cleanupForConcert(concertId);
      cleanupActiveForConcert(concertId);
    }
  }

  private void cleanupForConcert(Long concertId) {
    String queueKey = QueueRedisKey.queue(concertId);
    Set<Object> members = redisTemplate.opsForZSet().range(queueKey, 0, -1);

    if (members == null || members.isEmpty()) { // 대기열이 비어 있을 경우 불필요한 순회 방지
      return;
    }

    for (Object userId : members) {
      Long userIdLong = Long.valueOf(userId.toString());
      String heartbeatKey = QueueRedisKey.heartbeat(concertId, userIdLong);
      Boolean exists = redisTemplate.hasKey(heartbeatKey);

      if (Boolean.FALSE.equals(exists)) {
        redisTemplate.opsForZSet().remove(queueKey, userId);
      }
    }
  }

  private void cleanupActiveForConcert(Long concertId) {
    String activeKey = QueueRedisKey.active(concertId);
    Set<Object> activeUsers = redisTemplate.opsForSet().members(activeKey);

    if (activeUsers == null || activeUsers.isEmpty()) {
      return;
    }

    for (Object userId : activeUsers) {
      Long userIdLong = Long.valueOf(userId.toString());
      String tokenKey = QueueRedisKey.token(concertId, userIdLong);
      Boolean exists = redisTemplate.hasKey(tokenKey);

      if (Boolean.FALSE.equals(exists)) {
        redisTemplate.opsForSet().remove(activeKey, userIdLong);
        log.info("Active 이탈 처리: concertId={}, userId={}", concertId, userIdLong);
      }
    }
  }

  private Set<Long> getOpenConcertIds() {
    String key = QueueRedisKey.openConcerts();
    Set<Object> members = redisTemplate.opsForSet().members(key);

    if (members == null || members.isEmpty()) {
      return Collections.emptySet();
    }

    return members.stream().map(m -> Long.valueOf(m.toString())).collect(Collectors.toSet());
  }

  private void processQueueForConcert(Long concertId) {
    String activeKey = QueueRedisKey.active(concertId);
    Long currentActive = redisTemplate.opsForSet().size(activeKey);

    long availableSlots = MAX_ACTIVE_USERS - currentActive;
    if (availableSlots <= 0) { // 여유 슬롯이 없으면 접근할 수 없다.
      return;
    }

    String queueKey = QueueRedisKey.queue(concertId);
    Set<@NonNull TypedTuple<Object>> popped =
        redisTemplate.opsForZSet().popMin(queueKey, availableSlots);

    if (popped == null || popped.isEmpty()) {
      return; // 대기자 없으면 스킵
    }

    for (ZSetOperations.TypedTuple<Object> tuple : popped) {
      Long userId = Long.valueOf(tuple.getValue().toString());

      redisTemplate.opsForSet().add(activeKey, userId);

      String tokenKey = QueueRedisKey.token(concertId, userId);
      Map<String, Object> tokenData =
          Map.of("tokenId", UUID.randomUUID().toString(), "enteredAt", System.currentTimeMillis());
      redisTemplate.opsForValue().set(tokenKey, tokenData, Duration.ofMinutes(10));

      log.info("입장 처리: concertId={}, userId={}", concertId, userId);
    }
  }
}
