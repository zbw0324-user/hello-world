package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 登录入参
 */
@Data
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入登录账号")
    private String username;

    @NotBlank(message = "请输入登录密码")
    private String password;
}
