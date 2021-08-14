package com.rcloud.server.sealtalk.controller.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SendMessageAdminParam {
    @ApiModelProperty(value = "ZBG用户id列表，不能超过100个", required = true)
    private String[] userId;
    @ApiModelProperty(value = "消息内容", required = true)
    private String message;
    @ApiModelProperty(value = "附属信息")
    private String extra;
    @ApiModelProperty(value = "发送者ID")
    private String senderId;
}
