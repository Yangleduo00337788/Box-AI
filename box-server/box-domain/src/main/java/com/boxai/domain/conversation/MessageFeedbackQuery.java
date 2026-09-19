package com.boxai.domain.conversation;

import lombok.Data;

@Data
public class MessageFeedbackQuery {
    private String rating;
    private String status;
    private int page = 1;
    private int pageSize = 20;
}
