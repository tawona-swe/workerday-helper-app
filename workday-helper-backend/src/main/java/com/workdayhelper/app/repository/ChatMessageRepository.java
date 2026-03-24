package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.ChatMessage;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByUserOrderByCreatedAtAsc(User user);
}
