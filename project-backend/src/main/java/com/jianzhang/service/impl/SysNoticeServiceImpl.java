package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.entity.SysNotice;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.SysNoticeMapper;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 通知公告服务实现
 */
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper noticeMapper;

    @Override
    public PageResult<SysNotice> page(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysNotice::getTitle, keyword).or().like(SysNotice::getContent, keyword));
        }
        wrapper.orderByDesc(SysNotice::getNoticeId);
        Page<SysNotice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysNotice> published() {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getNoticeId)
                .last("LIMIT 8"));
    }

    @Override
    public void add(SysNotice notice) {
        checkNotice(notice);
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setNoticeId(null);
        noticeMapper.insert(notice);
    }

    @Override
    public void update(SysNotice notice) {
        if (notice.getNoticeId() == null) {
            throw new BusinessException("公告ID不能为空");
        }
        checkNotice(notice);
        noticeMapper.updateById(notice);
    }

    @Override
    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }

    private void checkNotice(SysNotice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BusinessException("公告标题不能为空");
        }
        if (!StringUtils.hasText(notice.getContent())) {
            throw new BusinessException("公告内容不能为空");
        }
        if (notice.getNoticeType() == null || (notice.getNoticeType() != 1 && notice.getNoticeType() != 2)) {
            notice.setNoticeType(1);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
    }
}
