package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

  @Query("""
      SELECT DISTINCT c FROM Channel c
      LEFT JOIN FETCH c.readStatuses rs
      LEFT JOIN FETCH rs.user u
      LEFT JOIN FETCH u.profile
      LEFT JOIN FETCH u.status
      WHERE c.type = :type
        OR EXISTS (
          SELECT 1 FROM ReadStatus rs2
          WHERE rs2.channel = c AND rs2.user.id = :userId
        )
      """)
  List<Channel> findAllByUserWithDetails(@Param("type") ChannelType type, @Param("userId") UUID userId);
}
