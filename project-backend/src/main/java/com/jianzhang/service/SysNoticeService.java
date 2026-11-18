package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.entity.SysNotice;

import java.util.List;

/**
 * 通知公告服务
 */
public interface SysNoticeService {

    /** 分页查询（管理端，含已关闭） */
    PageResult<SysNotice> page(Integer pageNum, Integer pageSize, String keyword);

    /** 已发布公告列表（所有登录用户可见，最新在前） */
    List<SysNotice> published();

    void add(SysNotice notice);

    void update(SysNotice notice);

    void delete(Long id);
}
