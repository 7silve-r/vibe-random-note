package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.common.exception.BusinessException;
import com.silver.music.entity.PlaylistBinding;
import com.silver.music.mapper.*;
import com.silver.music.service.PlaylistBindingService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaylistBindingServiceImpl extends ServiceImpl<PlaylistBindingMapper, PlaylistBinding>
        implements PlaylistBindingService {
    @Autowired private PlaylistMapper playlistMapper;
    @Autowired private SongMapper songMapper;
    @Autowired private PlaylistBindingMapper bindingMapper;

    @Override
    @Transactional
    public void update(Long id, List<Long> songIds) {
        if (playlistMapper.lock(id) == null) throw new BusinessException(404, "歌单不存在");
        if (songIds == null
                || songIds.size() > 1000
                || songIds.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new BusinessException("歌曲列表不合法或超过1000首");
        }
        List<Long> ids = songIds.stream().distinct().sorted().toList();
        for (Long songId : ids)
            if (songMapper.lock(songId) == null) throw new BusinessException(404, "歌曲不存在");
        bindingMapper.delete(
                new LambdaQueryWrapper<PlaylistBinding>().eq(PlaylistBinding::getPlaylistId, id));
        for (Long songId : ids) {
            PlaylistBinding binding = new PlaylistBinding();
            binding.setPlaylistId(id);
            binding.setSongId(songId);
            bindingMapper.insert(binding);
        }
    }
}
