package com.silver.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.poetry.config.PoetryDatasets;
import com.silver.poetry.entity.Poem;
import com.silver.poetry.mapper.*;
import com.silver.poetry.service.PoetryService;
import com.silver.poetry.vo.PoemVo;
import com.silver.poetry.vo.PoetryCatalogVo;
import com.silver.poetry.vo.PoetryGroup;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoetryServiceImpl implements PoetryService {
    @Autowired private PoemMapper poems;
    @Autowired private PoetMapper poets;
    @Autowired private PoetryFavoriteMapper favorites;
    @Autowired private PoetryDatasets datasets;

    @Override
    public PoetryCatalogVo catalog() {
        var groups = poems.groups();
        var totals =
                groups.stream()
                        .collect(
                                Collectors.groupingBy(
                                        PoetryGroup::dataset,
                                        Collectors.summingLong(PoetryGroup::total)));
        var collections =
                datasets.all().stream()
                        .filter(item -> totals.containsKey(item.code()))
                        .map(
                                item ->
                                        new PoetryCatalogVo.Collection(
                                                item.code(), item.name(), totals.get(item.code())))
                        .toList();
        return new PoetryCatalogVo(
                collections,
                groups.stream().map(PoetryGroup::dynasty).distinct().sorted().toList(),
                groups.stream().map(PoetryGroup::kind).distinct().sorted().toList());
    }

    @Override
    public PageResult<PoemVo> list(
            int pageNum,
            int pageSize,
            String keyword,
            String dynasty,
            String kind,
            String dataset,
            Integer authorId,
            boolean mine) {
        if (pageNum < 1 || pageNum > 100000 || pageSize < 1 || pageSize > 50)
            throw new BusinessException(400, "分页参数不正确");
        String term = keyword == null ? "" : keyword.strip();
        if (term.length() > 100) throw new BusinessException(400, "搜索内容不能超过100字");
        String collection = datasets.validate(dataset);
        var query = new LambdaQueryWrapper<Poem>();
        if (!term.isEmpty()) {
            String literal = term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
            query.apply(
                    "(title LIKE {0} ESCAPE '!' OR original_title LIKE {0} ESCAPE '!'"
                            + " OR author_name LIKE {0} ESCAPE '!')",
                    "%" + literal + "%");
        }
        query.eq(dynasty != null && !dynasty.isBlank(), Poem::getDynasty, dynasty)
                .eq(kind != null && !kind.isBlank(), Poem::getKind, kind)
                .eq(collection != null, Poem::getDataset, collection)
                .eq(authorId != null, Poem::getAuthorId, authorId);
        if (mine)
            query.inSql(
                    Poem::getId,
                    "SELECT poem_id FROM poetry_favorite WHERE user_id = " + SecurityUtil.userId());
        query.orderByAsc(Poem::getId);
        var page = poems.selectPage(new Page<>(pageNum, pageSize), query);
        return new PageResult<>(
                page.getTotal(),
                page.getRecords().stream()
                        .map(poem -> PoemVo.of(poem, mine, null, false))
                        .toList());
    }

    @Override
    public PoemVo detail(Integer id) {
        Poem poem = poems.selectById(id);
        if (poem == null) throw new BusinessException(404, "这首诗词不存在");
        Long uid = SecurityUtil.optionalUserId();
        var poet = poets.selectById(poem.getAuthorId());
        return PoemVo.of(
                poem,
                uid != null && favorites.count(uid, id) > 0,
                poet == null ? null : poet.getDescription(),
                true);
    }

    @Override
    public PoemVo random(Integer exclude, String dataset) {
        String collection = datasets.validate(dataset);
        var query =
                new LambdaQueryWrapper<Poem>()
                        .eq(collection != null, Poem::getDataset, collection)
                        .ne(exclude != null, Poem::getId, exclude);
        long count = poems.selectCount(query);
        if (count == 0 && exclude != null) return random(null, collection);
        if (count == 0) throw new BusinessException(404, "诗库还没有作品");
        query.select(Poem::getId)
                .orderByAsc(Poem::getId)
                .last("LIMIT 1 OFFSET " + ThreadLocalRandom.current().nextLong(count));
        Poem selected = poems.selectOne(query);
        if (selected == null) throw new BusinessException(404, "诗库正在更新，请稍后再试");
        return detail(selected.getId());
    }

    @Override
    public PoemVo daily() {
        var featured =
                poems.selectList(
                        new LambdaQueryWrapper<Poem>()
                                .select(Poem::getId)
                                .isNotNull(Poem::getFeaturedRank)
                                .isNotNull(Poem::getExcerpt)
                                .orderByAsc(Poem::getFeaturedRank));
        if (featured.isEmpty()) throw new BusinessException(404, "今日诗签尚未准备好");
        long day = LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay();
        return detail(featured.get(Math.floorMod(day, featured.size())).getId());
    }

    @Override
    @Transactional
    public void favorite(Integer id, boolean cancel) {
        Integer uid = SecurityUtil.userId();
        if (poems.lock(id) == null) throw new BusinessException(404, "这首诗词不存在");
        if (cancel) favorites.delete(uid, id);
        else if (favorites.count(uid.longValue(), id) == 0) favorites.insert(uid, id);
    }
}
