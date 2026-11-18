package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.dto.ApplyDTO;
import com.jianzhang.dto.WarehouseDTO;
import com.jianzhang.entity.StaffApply;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.StaffApplyMapper;
import com.jianzhang.security.LoginUser;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.StaffApplyService;
import com.jianzhang.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 员工领用申请服务实现
 * 流程：员工提交申请 -> 仓库角色审批 -> 通过后自动生成出库单并扣减库存
 */
@Service
@RequiredArgsConstructor
public class StaffApplyServiceImpl implements StaffApplyService {

    private final StaffApplyMapper applyMapper;
    private final WarehouseService warehouseService;

    @Override
    public PageResult<StaffApply> myPage(Integer pageNum, Integer pageSize, Integer status) {
        LoginUser user = SecurityUtils.getLoginUser();
        LambdaQueryWrapper<StaffApply> wrapper = new LambdaQueryWrapper<StaffApply>()
                .eq(StaffApply::getUserId, user.getUserId())
                .eq(status != null, StaffApply::getStatus, status)
                .orderByDesc(StaffApply::getApplyId);
        Page<StaffApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void submit(ApplyDTO dto) {
        if (!StringUtils.hasText(dto.getGoodsName())) {
            throw new BusinessException("请选择领用商品");
        }
        if (dto.getApplyNum() == null || dto.getApplyNum() <= 0) {
            throw new BusinessException("领用数量必须大于 0");
        }
        if (!StringUtils.hasText(dto.getReason())) {
            throw new BusinessException("请填写领用事由");
        }
        LoginUser user = SecurityUtils.getLoginUser();
        StaffApply apply = new StaffApply();
        apply.setUserId(user.getUserId());
        apply.setUsername(user.getUsername());
        apply.setNickname(user.getNickname());
        apply.setGoodsName(dto.getGoodsName().trim());
        apply.setApplyNum(dto.getApplyNum());
        apply.setReason(dto.getReason().trim());
        apply.setStatus(0);
        applyMapper.insert(apply);
    }

    @Override
    public void cancel(Long id) {
        StaffApply apply = mustOwn(id);
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw new BusinessException("仅待审批的申请可以撤回");
        }
        StaffApply update = new StaffApply();
        update.setApplyId(id);
        update.setStatus(3);
        applyMapper.updateById(update);
    }

    @Override
    public PageResult<StaffApply> auditPage(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<StaffApply> wrapper = new LambdaQueryWrapper<StaffApply>()
                .eq(status != null, StaffApply::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(StaffApply::getNickname, keyword)
                        .or().like(StaffApply::getGoodsName, keyword))
                .orderByAsc(StaffApply::getStatus)
                .orderByDesc(StaffApply::getApplyId);
        Page<StaffApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agree(Long id, String remark) {
        StaffApply apply = mustExist(id);
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw new BusinessException("该申请不在待审批状态");
        }
        // 审批通过即出库：复用仓库出库逻辑（含库存校验、台账重算）
        WarehouseDTO dto = new WarehouseDTO();
        dto.setType(2);
        dto.setGoodsName(apply.getGoodsName());
        dto.setGoodsNum(apply.getApplyNum());
        dto.setRecordTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        String applicant = StringUtils.hasText(apply.getNickname()) ? apply.getNickname() : apply.getUsername();
        dto.setRemark("领用申请 #" + id + "（" + applicant + "）"
                + (StringUtils.hasText(apply.getReason()) ? "：" + apply.getReason() : ""));
        warehouseService.add(dto);

        StaffApply update = new StaffApply();
        update.setApplyId(id);
        update.setStatus(1);
        update.setAuditBy(SecurityUtils.getLoginUser().getNickname());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(StringUtils.hasText(remark) ? remark : "同意领用");
        applyMapper.updateById(update);
    }

    @Override
    public void reject(Long id, String remark) {
        StaffApply apply = mustExist(id);
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw new BusinessException("该申请不在待审批状态");
        }
        StaffApply update = new StaffApply();
        update.setApplyId(id);
        update.setStatus(2);
        update.setAuditBy(SecurityUtils.getLoginUser().getNickname());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(StringUtils.hasText(remark) ? remark : "驳回");
        applyMapper.updateById(update);
    }

    @Override
    public Long countPending() {
        return applyMapper.selectCount(new LambdaQueryWrapper<StaffApply>()
                .eq(StaffApply::getStatus, 0));
    }

    @Override
    public Long countMyPending() {
        LoginUser user = SecurityUtils.getLoginUser();
        return applyMapper.selectCount(new LambdaQueryWrapper<StaffApply>()
                .eq(StaffApply::getUserId, user.getUserId())
                .eq(StaffApply::getStatus, 0));
    }

    // ================= 内部方法 =================

    private StaffApply mustExist(Long id) {
        StaffApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("领用申请不存在");
        }
        return apply;
    }

    private StaffApply mustOwn(Long id) {
        StaffApply apply = mustExist(id);
        LoginUser user = SecurityUtils.getLoginUser();
        if (!user.getUserId().equals(apply.getUserId())) {
            throw new BusinessException("只能操作本人提交的申请");
        }
        return apply;
    }
}
