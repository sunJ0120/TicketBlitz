package com.example.be.concert.repository;

import com.example.be.concert.domain.HallSeatPosition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallSeatPositionRepository extends JpaRepository<HallSeatPosition, Long> {

  Optional<List<HallSeatPosition>> findByHallTemplateId(Long id);
}
