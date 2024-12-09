package com.sinuohao.repository;

import com.sinuohao.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query(value = "SELECT * FROM messages WHERE (:query IS NULL OR content LIKE %:query%) " +
           "ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Message> searchMessages(@Param("query") String query, @Param("offset") int offset, @Param("limit") int limit);
}
