package com.rcloud.server.sealtalk.controller.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("查询用户列表入参")
public class UserListParam {
    @ApiModelProperty("账户id")
    private String id;
    @ApiModelProperty("ZBG用户id")
    private String userId;
    @ApiModelProperty(value = "昵称")
    private String nickname;
    @ApiModelProperty(value = "查询开始时间")
    private String startTime;
    @ApiModelProperty(value = "查询结束时间")
    private String endTime;
    @ApiModelProperty(value = "页码")
    private Integer pageNum;
    @ApiModelProperty(value = "返回数据数")
    private Integer pageSize;
}
