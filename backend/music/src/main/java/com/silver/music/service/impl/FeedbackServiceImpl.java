package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.FeedbackQueryDto;
import com.silver.music.entity.Feedback;
import com.silver.music.mapper.FeedbackMapper;
import com.silver.music.service.FeedbackService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback>
        implements FeedbackService {

    @Autowired private FeedbackMapper feedbackMapper;

    @Override
    public PageResult<Feedback> listFeedback(FeedbackQueryDto feedbackDto) {

        Page<Feedback> page = new Page<>(feedbackDto.getPageNum(), feedbackDto.getPageSize());
        LambdaQueryWrapper<Feedback> query = new LambdaQueryWrapper<>();
        if (feedbackDto.getKeyword() != null) {
            query.like(Feedback::getFeedback, feedbackDto.getKeyword());
        }

        query.orderByDesc(Feedback::getCreateTime);

        IPage<Feedback> feedbackPage = feedbackMapper.selectPage(page, query);

        return new PageResult<>(feedbackPage.getTotal(), feedbackPage.getRecords());
    }

    @Override
    public void deleteFeedback(Long feedbackId) {
        if (feedbackMapper.deleteById(feedbackId) == 0) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    public void deleteFeedbacks(List<Long> feedbackIds) {
        if (feedbackMapper.deleteByIds(feedbackIds) == 0) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    public void addFeedback(String content) {
        if (content == null || content.isBlank() || content.length() > 255)
            throw new BusinessException("反馈须为1到255字");
        Long userId = SecurityUtil.userId().longValue();

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setFeedback(content);
        feedback.setCreateTime(LocalDateTime.now());

        if (feedbackMapper.insert(feedback) == 0) {
            throw new BusinessException("添加失败");
        }
    }
}
