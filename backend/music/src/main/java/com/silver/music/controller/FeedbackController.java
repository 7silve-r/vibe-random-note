package com.silver.music.controller;

import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.music.dto.FeedbackQueryDto;
import com.silver.music.entity.Feedback;
import com.silver.music.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class FeedbackController {

    @Autowired private FeedbackService feedbackService;

    @PostMapping("/music/admin/listFeedback")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Feedback>> listFeedback(
            @RequestBody @Valid FeedbackQueryDto feedbackDto) {
        return Result.success(feedbackService.listFeedback(feedbackDto));
    }

    @DeleteMapping("/music/admin/deleteFeedback/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteFeedback(@PathVariable("id") Long feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return Result.success();
    }

    @DeleteMapping("/music/admin/deleteFeedbacks")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteFeedbacks(@RequestBody List<Long> feedbackIds) {
        feedbackService.deleteFeedbacks(feedbackIds);
        return Result.success();
    }

    @PostMapping("/music/feedback/addFeedback")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> addFeedback(@RequestParam(value = "content") String content) {
        feedbackService.addFeedback(content);
        return Result.success();
    }
}
