package com.jianzhang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianzhang.entity.StaffApply;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工领用申请 Mapper
 */
@Mapper
public interface StaffApplyMapper extends BaseMapper<StaffApply> {
}
