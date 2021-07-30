package com.rcloud.server.sealtalk.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/25
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Data
public class MemberDTO {
    @ApiModelProperty("用户群昵称")
    private String groupNickname;
    @ApiModelProperty("用户角色，0:创建者，1：普通成员，2：管理员")
    private Integer role;
    @ApiModelProperty("创建时间，字符串")
    private String createdAt;
    @ApiModelProperty("创建时间，时间戳")
    private Long createdTime;
    @ApiModelProperty("更新时间，字符串")
    private String updatedAt;
    @ApiModelProperty("更新时间，时间戳")
    private Long updatedTime;

    private UserDTO user;
}
