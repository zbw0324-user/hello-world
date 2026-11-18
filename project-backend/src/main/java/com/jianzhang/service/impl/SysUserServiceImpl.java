package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.dto.UserDTO;
import com.jianzhang.entity.SysRole;
import com.jianzhang.entity.SysUser;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.SysRoleMapper;
import com.jianzhang.mapper.SysUserMapper;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.SysUserService;
import com.jianzhang.util.DateUtils;
import com.jianzhang.util.ExcelUtil;
import com.jianzhang.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 默认初始密码 */
    private static final String DEFAULT_PASSWORD = "123456";
    /** 受保护的超级管理员角色标识 */
    private static final String ADMIN_ROLE_CODE = "admin";
    /** 用户导入表头 */
    private static final List<String> HEADERS = Collections.unmodifiableList(Arrays.asList(
            "登录账号", "用户昵称", "角色标识", "初始密码", "账号状态", "备注"));

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUser> page(Integer pageNum, Integer pageSize, String keyword, Long roleId, Integer status) {
        Page<SysUser> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<SysUser> wrapper = buildWrapper(keyword, roleId, status)
                .orderByAsc(SysUser::getUserId);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        // 填充角色名称，避免前端二次请求
        Map<Long, SysRole> roleMap = roleMap();
        result.getRecords().forEach(user -> fillRole(user, roleMap));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(UserDTO dto) {
        checkUsernameUnique(dto.getUsername(), null);
        SysRole role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException("所选角色不存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setNickname(dto.getNickname().trim());
        user.setRoleId(dto.getRoleId());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
        String password = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD;
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO dto) {
        if (dto.getUserId() == null) {
            throw new BusinessException("缺少用户ID");
        }
        SysUser exist = userMapper.selectById(dto.getUserId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        checkUsernameUnique(dto.getUsername(), dto.getUserId());
        if (roleMapper.selectById(dto.getRoleId()) == null) {
            throw new BusinessException("所选角色不存在");
        }
        SysUser update = new SysUser();
        update.setUserId(dto.getUserId());
        update.setUsername(dto.getUsername().trim());
        update.setNickname(dto.getNickname().trim());
        update.setRoleId(dto.getRoleId());
        update.setStatus(dto.getStatus());
        update.setRemark(dto.getRemark());
        if (StringUtils.hasText(dto.getPassword())) {
            if (dto.getPassword().length() < 6) {
                throw new BusinessException("密码长度不能少于 6 位");
            }
            update.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        // 不允许把自己降级为非管理员
        if (dto.getUserId().equals(SecurityUtils.getUserId())) {
            update.setRoleId(exist.getRoleId());
        }
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (userId == null) {
            throw new BusinessException("缺少用户ID");
        }
        if (userId.equals(SecurityUtils.getUserId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role != null && ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new BusinessException("超级管理员账号不允许删除");
        }
        userMapper.deleteById(userId);
    }

    @Override
    public void resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(update);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("账号状态不合法");
        }
        if (userId.equals(SecurityUtils.getUserId()) && status == 0) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getRoleId));
    }

    // ================= 导出 / 导入 =================

    @Override
    public void export(String keyword, Long roleId, Integer status, HttpServletResponse response) throws IOException {
        List<SysUser> users = userMapper.selectList(buildWrapper(keyword, roleId, status)
                .orderByAsc(SysUser::getUserId));
        Map<Long, SysRole> roleMap = roleMap();
        List<List<Object>> rows = new ArrayList<>();
        for (SysUser user : users) {
            fillRole(user, roleMap);
            rows.add(Arrays.asList(
                    user.getUsername(),
                    user.getNickname(),
                    user.getRoleName(),
                    user.getRoleCode(),
                    user.getStatus() != null && user.getStatus() == 1 ? "正常" : "禁用",
                    DateUtils.format(user.getCreateTime()),
                    user.getRemark()
            ));
        }
        List<String> exportHeaders = Arrays.asList("登录账号", "用户昵称", "角色名称", "角色标识", "账号状态", "创建时间", "备注");
        ExcelUtil.export(response, "用户列表_" + timestamp(), exportHeaders, rows);
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        List<List<Object>> samples = new ArrayList<>();
        samples.add(Arrays.asList("emp10", "赵一鸣", "staff", "123456", "正常", "销售部员工"));
        samples.add(Arrays.asList("wh02", "孙晓芸", "warehouse", "123456", "正常", "华东仓管员"));
        ExcelUtil.exportTemplate(response, "用户导入模板", HEADERS, samples,
                "「登录账号」必填，3-20 位，全系统唯一，导入后用于登录",
                "「用户昵称」必填，最多 50 个字符",
                "「角色标识」必填，可填 admin / manager / finance / warehouse / staff",
                "「初始密码」选填，留空默认为 123456，长度不少于 6 位",
                "「账号状态」可填 正常 / 禁用（也支持 1 / 0），留空默认正常",
                "「备注」选填，最多 255 个字符",
                "单次最多导入 500 个账号，账号重复的行会被跳过并给出原因");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        ImportResultVO result = ImportResultVO.of();
        List<List<String>> rows = ExcelUtil.read(file);
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中没有可导入的数据，请使用系统模板填写");
        }
        if (looksLikeHeader(rows.get(0))) {
            rows = rows.subList(1, rows.size());
        }
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中只有表头，没有数据行");
        }
        if (rows.size() > 500) {
            throw new BusinessException("单次最多导入 500 个账号，请拆分文件");
        }
        Map<String, SysRole> roleByCode = roleMap().values().stream()
                .collect(Collectors.toMap(role -> role.getRoleCode().toLowerCase(), Function.identity(), (a, b) -> a));
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 2;
            try {
                UserDTO dto = convert(rows.get(i), roleByCode);
                add(dto);
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO batchAdd(List<UserDTO> list) {
        ImportResultVO result = ImportResultVO.of();
        if (list == null || list.isEmpty()) {
            throw new BusinessException("请至少填写一行数据");
        }
        if (list.size() > 500) {
            throw new BusinessException("单次最多新增 500 个账号");
        }
        for (int i = 0; i < list.size(); i++) {
            UserDTO dto = list.get(i);
            int rowNo = i + 1;
            try {
                if (dto == null) {
                    throw new BusinessException("整行为空");
                }
                if (!StringUtils.hasText(dto.getUsername())) {
                    throw new BusinessException("登录账号不能为空");
                }
                if (dto.getUsername().trim().length() < 3 || dto.getUsername().trim().length() > 20) {
                    throw new BusinessException("登录账号长度需为 3-20 位");
                }
                if (!StringUtils.hasText(dto.getNickname())) {
                    throw new BusinessException("用户昵称不能为空");
                }
                if (dto.getRoleId() == null) {
                    throw new BusinessException("请选择所属角色");
                }
                add(dto);
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        return result;
    }

    // ================= 内部方法 =================

    private LambdaQueryWrapper<SysUser> buildWrapper(String keyword, Long roleId, Integer status) {
        return new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .eq(roleId != null, SysUser::getRoleId, roleId)
                .eq(status != null, SysUser::getStatus, status);
    }

    private Map<Long, SysRole> roleMap() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleId, Function.identity(), (a, b) -> a));
    }

    private void fillRole(SysUser user, Map<Long, SysRole> roleMap) {
        SysRole role = roleMap.get(user.getRoleId());
        if (role != null) {
            user.setRoleName(role.getRoleName());
            user.setRoleCode(role.getRoleCode());
        }
    }

    private boolean looksLikeHeader(List<String> first) {
        return !first.isEmpty() && first.get(0).replace(" ", "").contains("账号");
    }

    private UserDTO convert(List<String> row, Map<String, SysRole> roleByCode) {
        String username = cell(row, 0);
        String nickname = cell(row, 1);
        String roleCode = cell(row, 2);
        String password = cell(row, 3);
        String statusText = cell(row, 4);
        String remark = cell(row, 5);

        if (!StringUtils.hasText(username)) {
            throw new BusinessException("登录账号不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException("登录账号长度需为 3-20 位");
        }
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException("用户昵称不能为空");
        }
        if (nickname.length() > 50) {
            throw new BusinessException("用户昵称不能超过 50 个字符");
        }
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException("角色标识不能为空");
        }
        SysRole role = roleByCode.get(roleCode.trim().toLowerCase());
        if (role == null) {
            throw new BusinessException("角色标识不存在：" + roleCode);
        }
        if (StringUtils.hasText(password) && password.length() < 6) {
            throw new BusinessException("初始密码长度不能少于 6 位");
        }
        if (remark.length() > 255) {
            throw new BusinessException("备注不能超过 255 个字符");
        }
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setNickname(nickname);
        dto.setRoleId(role.getRoleId());
        dto.setPassword(StringUtils.hasText(password) ? password : DEFAULT_PASSWORD);
        dto.setStatus(parseStatus(statusText));
        dto.setRemark(StringUtils.hasText(remark) ? remark : null);
        return dto;
    }

    private Integer parseStatus(String text) {
        if (!StringUtils.hasText(text)) {
            return 1;
        }
        String value = text.trim();
        if ("0".equals(value) || "禁用".equals(value) || "停用".equals(value)) {
            return 0;
        }
        return 1;
    }

    private String cell(List<String> row, int index) {
        return index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private String message(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : "数据格式不正确";
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .ne(excludeId != null, SysUser::getUserId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("登录账号已存在，请更换");
        }
    }
}
