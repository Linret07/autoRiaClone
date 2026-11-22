package com.autoriaclone.service;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class ProfanityFilterService {
    private static final Set<String> FORBIDDEN_WORDS = Set.of(
            "badword", "spam", "scam", "дурень", "лох", "кидалово", "матюк"
    );

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return FORBIDDEN_WORDS.stream().anyMatch(lower::contains);
    }
}
