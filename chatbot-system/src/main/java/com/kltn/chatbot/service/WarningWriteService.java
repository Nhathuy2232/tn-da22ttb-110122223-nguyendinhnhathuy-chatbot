package com.kltn.chatbot.service;

import com.kltn.chatbot.model.entity.Warning;
import com.kltn.chatbot.repository.WarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi warning trong transaction riêng để lỗi một bản ghi không làm hỏng batch.
 */
@Service
@RequiredArgsConstructor
public class WarningWriteService {

    private final WarningRepository warningRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Warning save(Warning warning) {
        return warningRepository.save(warning);
    }
}
