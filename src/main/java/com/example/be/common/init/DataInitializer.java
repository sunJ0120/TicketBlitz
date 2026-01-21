package com.example.be.common.init;

import com.example.be.concert.domain.*;
import com.example.be.concert.enums.ConcertStatus;
import com.example.be.concert.enums.Genre;
import com.example.be.concert.enums.SeatLabel;
import com.example.be.concert.enums.SeatStatus;
import com.example.be.concert.repository.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final ConcertRepository concertRepository;
  private final HallTemplateRepository hallTemplateRepository;
  private final HallSeatPositionRepository positionRepository;
  private final BuildingRepository buildingRepository;
  private final ConcertSeatRepository concertSeatRepository;

  @Override
  @Transactional
  public void run(String[] args) {
    // 1. 빌딩과 템플릿이 없으면 생성, 있으면 가져오기
    HallTemplate template = createSampleHallTemplate();
    List<HallSeatPosition> positions = createSampleHallSeatPosition(template);

    Genre[] genres = Genre.values();
    ConcertStatus[] statuses = {
      ConcertStatus.SCHEDULED,
      ConcertStatus.BOOKING_OPEN,
      ConcertStatus.BOOKING_OPEN, // 비중 높게
      ConcertStatus.BOOKING_OPEN,
      ConcertStatus.SOLD_OUT
    };

    String[] artists = {"BTS", "IU", "에스파", "뉴진스", "세븐틴", "블랙핑크", "아이유", "임영웅", "성시경", "폴킴"};
    String[] titles = {"월드투어", "단독 콘서트", "팬미팅", "앵콜 콘서트", "연말 콘서트"};

    for (int i = 1; i <= 50; i++) {
      // 섹션별 가격 설정
      int vipPrice = 150000 + (i % 5) * 10000;
      int rPrice = 100000 + (i % 5) * 10000;
      int sPrice = 50000 + (i % 5) * 10000;

      List<ConcertSection> sections =
          List.of(
              ConcertSection.builder()
                  .sectionLabel(SeatLabel.VIP)
                  .rowStart(1)
                  .rowEnd(5)
                  .price(vipPrice)
                  .build(),
              ConcertSection.builder()
                  .sectionLabel(SeatLabel.R)
                  .rowStart(6)
                  .rowEnd(15)
                  .price(rPrice)
                  .build(),
              ConcertSection.builder()
                  .sectionLabel(SeatLabel.S)
                  .rowStart(16)
                  .rowEnd(25)
                  .price(sPrice)
                  .build());

      String artist = artists[i % artists.length];
      String title = artist + " " + titles[i % titles.length];

      Concert concert =
          Concert.builder()
              .hallTemplate(template)
              .title(title)
              .artist(artist)
              .genre(genres[i % genres.length])
              .concertStatus(statuses[i % statuses.length])
              .viewCount((long) (Math.random() * 50000))
              .startDate(LocalDateTime.now().plusDays(i))
              .endDate(LocalDateTime.now().plusDays(i + 2))
              .bookingStartAt(LocalDateTime.now().minusDays(1))
              .bookingEndAt(LocalDateTime.now().plusDays(i))
              .concertSections(sections)
              .minPrice(sPrice)
              .maxPrice(vipPrice)
              .build();

      concertRepository.save(concert);
      createSampleSeatsForConcert(concert, positions);
    }
  }

  private HallTemplate createSampleHallTemplate() {
    return hallTemplateRepository
        .findById(1L)
        .orElseGet(
            () -> {
              Building building =
                  buildingRepository.save(
                      Building.builder()
                          .name("서울 올림픽 홀")
                          .address("서울특별시 송파구 올림픽로 424")
                          .latitude(37.520)
                          .longitude(127.127)
                          .build());

              // 2. 저장된 빌딩 객체를 템플릿에 넣습니다.
              return hallTemplateRepository.save(
                  HallTemplate.builder()
                      .hallName("Main Hall")
                      .totalSeats(1000)
                      .totalRows(25)
                      .building(building) // 여기서 building은 이미 DB에 저장된 상태
                      .build());
            });
  }

  private List<HallSeatPosition> createSampleHallSeatPosition(HallTemplate template) {
    return positionRepository
        .findByHallTemplateId(1L)
        .orElseGet(
            () -> {
              List<HallSeatPosition> positions = new ArrayList<>();
              for (int row = 1; row <= 25; row++) {
                for (int seat = 1; seat <= 40; seat++) {
                  positions.add(
                      positionRepository.save(
                          HallSeatPosition.builder()
                              .hallTemplate(template)
                              .rowNum(row)
                              .seatNum(seat)
                              .xCoord((double) seat)
                              .yCoord((double) row)
                              .build()));
                }
              }

              return positions;
            });
  }

  private void createSampleSeatsForConcert(Concert concert, List<HallSeatPosition> positions) {
    List<ConcertSection> sections = concert.getConcertSections();

    List<ConcertSeat> concertSeats =
        positions.stream()
            .map(
                pos -> {
                  ConcertSection matchedSection =
                      sections.stream()
                          .filter(
                              section ->
                                  pos.getRowNum() >= section.getRowStart()
                                      && pos.getRowNum() <= section.getRowEnd())
                          .findFirst()
                          .orElse(null);

                  if (matchedSection == null) {
                    return null;
                  }

                  return ConcertSeat.builder()
                      .concert(concert)
                      .hallSeatPosition(pos)
                      .sectionLabel(matchedSection.getSectionLabel())
                      .seatStatus(SeatStatus.AVAILABLE)
                      .build();
                })
            .filter(Objects::nonNull)
            .toList();

    concertSeatRepository.saveAll(concertSeats);
  }
}
