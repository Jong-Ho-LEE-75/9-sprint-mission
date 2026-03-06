package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

  @Query("""
      SELECT m FROM Message m
      JOIN FETCH m.author a
      LEFT JOIN FETCH a.profile
      LEFT JOIN FETCH a.status
      WHERE m.channel.id = :channelId
      ORDER BY m.createdAt DESC
      """)
  Slice<Message> findAllByChannelId(
      @Param("channelId") UUID channelId,
      Pageable pageable);

  @Query("""
      SELECT m FROM Message m
      JOIN FETCH m.author a
      LEFT JOIN FETCH a.profile
      LEFT JOIN FETCH a.status
      WHERE m.channel.id = :channelId
        AND m.createdAt < :cursor
      ORDER BY m.createdAt DESC
      """)
  Slice<Message> findAllByChannelIdBeforeCursor(
      @Param("channelId") UUID channelId,
      @Param("cursor") Instant cursor,
      Pageable pageable);

  List<Message> findAllByChannel_Id(UUID channelId);

  void deleteAllByChannel_Id(UUID channelId);

  @Query("SELECT MAX(m.createdAt) FROM Message m WHERE m.channel.id = :channelId")
  Optional<Instant> findLastCreatedAtByChannelId(@Param("channelId") UUID channelId);

  @Query("SELECT m.channel.id, MAX(m.createdAt) FROM Message m WHERE m.channel.id IN :channelIds GROUP BY m.channel.id")
  List<Object[]> findLastCreatedAtByChannelIds(@Param("channelIds") List<UUID> channelIds);
}
