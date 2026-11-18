package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.dto.UserDTO;
import com.jianzhang.entity.SysRole;
import com.jianzhang.entity.SysUser;
import com.jianzhang.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 用户管理服务（管理员使用）
 */
public interface SysUserService {

    PageResult<SysUser> page(Integer pageNum, Integer pageSize, String keyword, Long roleId, Integer status);

    void add(UserDTO dto);

    void update(UserDTO dto);

    void delete(Long userId);

    void resetPassword(Long userId);

    void updateStatus(Long userId, Integer status);

    List<SysRole> listRoles();

    /**
     * 按筛选条件导出用户列表
     */
    void export(String keyword, Long roleId, Integer status, HttpServletResponse response) throws IOException;

    /**
     * 下载用户导入模板
     */
    void template(HttpServletResponse response) throws IOException;

    /**
     * 从 Excel 批量导入用户
     */
    ImportResultVO importExcel(MultipartFile file) throws IOException;

    /**
     * 批量新增用户（前端可编辑表格提交）
     */
    ImportResultVO batchAdd(List<UserDTO> list);
}
