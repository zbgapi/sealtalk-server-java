package com.rcloud.server.sealtalk.controller.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("创建群组入参")
public class GroupCreateParam {

    @ApiModelProperty(value = "群组名", required = true)
    private String name;
    @ApiModelProperty(value = "群主ZBG用户id", required = true)
    private String creatorUid;
    @ApiModelProperty(value = "群头像地址")
    private String portraitUri;
    @ApiModelProperty(value = "群成员id，zbg用户id")
    private String[] userId;
    @ApiModelProperty(value = "群成员数量限制")
    private Integer maxMemberCount;

    @ApiModelProperty(value = "入群持有币种名限制")
    private String currencyName;
    @ApiModelProperty(value = "入群持有币种数量")
    private BigDecimal amount;
    @ApiModelProperty(value = "入群验证码")
    private String verificationCode;

    @ApiModelProperty(value = "关联交易对")
    private String marketName;
    @ApiModelProperty(value = "是否推荐群， 1/0")
    private Integer referFlag;
    @ApiModelProperty(value = "是否推荐群， 1/0")
    private Integer hotFlag;
}
