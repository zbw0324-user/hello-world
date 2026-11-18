package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.dto.ApplyDTO;
import com.jianzhang.entity.StaffApply;

/**
 * 员工领用申请服务
 */
public interface StaffApplyService {

    /**
     * 我的申请分页（当前登录用户）
     */
    PageResult<StaffApply> myPage(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 提交领用申请
     */
    void submit(ApplyDTO dto);

    /**
     * 撤回申请（仅待审批状态、本人申请）
     */
    void cancel(Long id);

    /**
     * 审批列表（仓库/管理层视角，可按状态筛选）
     */
    PageResult<StaffApply> auditPage(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /**
     * 通过申请：自动生成出库单并回填 recordId
     */
    void agree(Long id, String remark);

    /**
     * 驳回申请
     */
    void reject(Long id, String remark);

    /**
     * 待审批申请数（看板用）
     */
    Long countPending();

    /**
     * 当前用户待审批申请数（看板用）
     */
    Long countMyPending();
}
