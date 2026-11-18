package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.PageResult;
import com.jianzhang.common.Result;
import com.jianzhang.dto.RoleDTO;
import com.jianzhang.dto.UserDTO;
import com.jianzhang.entity.SysLog;
import com.jianzhang.entity.SysRole;
import com.jianzhang.entity.SysUser;
import com.jianzhang.service.SysLogService;
import com.jianzhang.service.SysMenuService;
import com.jianzhang.service.SysRoleService;
import com.jianzhang.service.SysUserService;
import com.jianzhang.vo.ImportResultVO;
import com.jianzhang.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * 系统管理接口（仅超级管理员）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final SysLogService logService;

    // ==================== 用户管理 ====================

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:list')")
    @GetMapping("/users")
    public Result<PageResult<SysUser>> userPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long roleId,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(userService.page(pageNum, pageSize, keyword, roleId, status));
    }

    /**
     * 导出用户列表（支持当前筛选条件）
     */
    @Log(module = "系统管理", value = "导出用户列表")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:export')")
    @GetMapping("/users/export")
    public void exportUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long roleId,
                            @RequestParam(required = false) Integer status,
                            HttpServletResponse response) throws IOException {
        userService.export(keyword, roleId, status, response);
    }

    /**
     * 下载用户导入模板
     */
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:import')")
    @GetMapping("/users/template")
    public void userTemplate(HttpServletResponse response) throws IOException {
        userService.template(response);
    }

    /**
     * Excel 批量导入用户
     */
    @Log(module = "系统管理", value = "导入用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:import')")
    @PostMapping("/users/import")
    public Result<ImportResultVO> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResultVO result = userService.importExcel(file);
        return Result.success(result.summary(), result);
    }

    /**
     * 批量新增用户（前端可编辑表格）
     */
    @Log(module = "系统管理", value = "批量新增用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:batch')")
    @PostMapping("/users/batch")
    public Result<ImportResultVO> batchUsers(@RequestBody List<UserDTO> list) {
        ImportResultVO result = userService.batchAdd(list);
        return Result.success(result.summary(), result);
    }

    @Log(module = "系统管理", value = "新增用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:add')")
    @PostMapping("/users")
    public Result<Void> addUser(@Valid @RequestBody UserDTO dto) {
        userService.add(dto);
        return Result.success("新增用户成功", null);
    }

    @Log(module = "系统管理", value = "编辑用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:edit')")
    @PutMapping("/users")
    public Result<Void> updateUser(@Valid @RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.success("修改用户成功", null);
    }

    @Log(module = "系统管理", value = "删除用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:remove')")
    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        userService.delete(userId);
        return Result.success("删除用户成功", null);
    }

    @Log(module = "系统管理", value = "重置用户密码")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:reset')")
    @PutMapping("/users/{userId}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return Result.success("密码已重置为 123456", null);
    }

    @Log(module = "系统管理", value = "修改用户状态")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:edit')")
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userService.updateStatus(userId, status);
        return Result.success(status == 1 ? "账号已启用" : "账号已禁用", null);
    }

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:list')")
    @GetMapping("/roles/options")
    public Result<List<SysRole>> roleOptions() {
        return Result.success(userService.listRoles());
    }

    // ==================== 角色与权限 ====================

    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('system:role:list','system:user:list')")
    @GetMapping("/roles")
    public Result<List<SysRole>> roleList() {
        return Result.success(roleService.list());
    }

    @Log(module = "系统管理", value = "新增角色")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:role:add')")
    @PostMapping("/roles")
    public Result<Void> addRole(@Valid @RequestBody RoleDTO dto) {
        roleService.add(dto);
        return Result.success("新增角色成功", null);
    }

    @Log(module = "系统管理", value = "编辑角色")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:role:edit')")
    @PutMapping("/roles")
    public Result<Void> updateRole(@Valid @RequestBody RoleDTO dto) {
        roleService.update(dto);
        return Result.success("修改角色成功", null);
    }

    @Log(module = "系统管理", value = "删除角色")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:role:remove')")
    @DeleteMapping("/roles/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        roleService.delete(roleId);
        return Result.success("删除角色成功", null);
    }

    /**
     * 角色已授权的菜单ID（树形勾选回显）
     */
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('system:role:list','system:role:assign')")
    @GetMapping("/roles/{roleId}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long roleId) {
        return Result.success(roleService.menuIds(roleId));
    }

    /**
     * 为角色分配菜单权限
     */
    @Log(module = "系统管理", value = "分配角色权限")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:role:assign')")
    @PutMapping("/roles/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody RoleDTO dto) {
        dto.setRoleId(roleId);
        roleService.assignMenus(dto);
        return Result.success("权限分配成功", null);
    }

    /**
     * 全部菜单树（含按钮，用于授权面板）
     */
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('system:role:list','system:role:assign')")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menuTree() {
        return Result.success(menuService.getAllMenuTree());
    }

    // ==================== 操作日志 ====================

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:log:list')")
    @GetMapping("/logs")
    public Result<PageResult<SysLog>> logPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime) {
        return Result.success(logService.page(pageNum, pageSize, keyword, startTime, endTime));
    }

    @Log(module = "系统管理", value = "清空操作日志")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:log:list')")
    @DeleteMapping("/logs")
    public Result<Void> clearLogs() {
        logService.clear();
        return Result.success("日志已清空", null);
    }

    /**
     * 操作日志导出 Excel（沿用当前筛选条件，最多 5000 条）
     */
    @Log(module = "系统管理", value = "导出操作日志")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:log:export')")
    @GetMapping("/logs/export")
    public void exportLogs(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String startTime,
                           @RequestParam(required = false) String endTime,
                           javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        logService.export(keyword, startTime, endTime, response);
    }
}
