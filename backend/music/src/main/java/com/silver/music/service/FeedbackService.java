package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.FeedbackQueryDto;
import com.silver.music.entity.Feedback;
import java.util.List;

public interface FeedbackService extends IService<Feedback> {

    PageResult<Feedback> listFeedback(FeedbackQueryDto feedbackDto);

    void deleteFeedback(Long feedbackId);

    void deleteFeedbacks(List<Long> feedbackIds);

    void addFeedback(String content);
}
