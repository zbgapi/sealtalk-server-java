package com.rcloud.server.sealtalk.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.google.common.collect.ImmutableList;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.controller.param.*;
import com.rcloud.server.sealtalk.domain.GroupBulletins;
import com.rcloud.server.sealtalk.domain.GroupMembers;
import com.rcloud.server.sealtalk.domain.Groups;
import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.manager.GroupManager;
import com.rcloud.server.sealtalk.manager.MiscManager;
import com.rcloud.server.sealtalk.manager.UserManager;
import com.rcloud.server.sealtalk.model.dto.*;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import io.rong.models.BlockUsers;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Api(tags = "后台管理相关")
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController extends BaseController {

    @Resource
    private GroupManager groupManager;
    @Resource
    private UserManager userManager;
    @Resource
    private MiscManager miscManager;

    @ApiOperation(value = "发送系统消息")
    @RequestMapping(value = "/send_message", method = RequestMethod.POST)
    public APIResult<Object> sendMessage(@RequestBody SendMessageAdminParam param) throws ServiceException {
        String[] userIds = param.getUserId();
        String message = param.getMessage();

        ValidateUtils.notEmpty(message);
        ValidateUtils.notEmpty(userIds);


        List<Integer> ids = userManager.batchGetOrRegisterUsers(userIds);
        if (ids.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }
        Integer[] decodeMemberIds = ids.toArray(new Integer[0]);

        miscManager.sendSystemMessage(message, decodeMemberIds);
        return APIResultWrap.ok("发送系统消息成功");
    }


    @ApiOperation(value = "获取用户列表")
    @RequestMapping(value = "/user/list", method = RequestMethod.GET)
    public APIResult<Page<UserDTO>> getUsers(UserListParam param) throws ServiceException {

        Page<Users> page = userManager.getUserList(param);

        Page<UserDTO> resultPage = new Page<>(page.getPageNum(), page.getPageSize());
        resultPage.setTotal(page.getTotal());
        if (resultPage.getTotal() > 0) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            List<BlockUsers> blockUserList = userManager.getBlockUserList();
            Set<String> blockIds = blockUserList.stream().map(BlockUsers::getId).collect(Collectors.toSet());
            for (Users u : page) {
                UserDTO userDTO = new UserDTO();
                userDTO.setId(N3d.encode(u.getId()));
                userDTO.setNickname(u.getNickname());
                userDTO.setRegion(u.getRegion());
                userDTO.setPhone(u.getPhone());
                userDTO.setGender(u.getGender());
                userDTO.setPortraitUri(u.getPortraitUri());
                userDTO.setStAccount(u.getStAccount());
                userDTO.setBlockStatus(blockIds.contains(userDTO.getId()) ? 1 : 0);
                userDTO.setCreateAt(format.format(u.getCreatedAt()));
                resultPage.add(userDTO);
            }
        }
        return APIResultWrap.ok(resultPage);
    }

    @ApiOperation(value = "设置用户禁封状态")
    @RequestMapping(value = "/user/set_block", method = RequestMethod.POST)
    public APIResult<Object> setBlock(@RequestBody UserBlockParam param) throws ServiceException {
        String id = param.getId();
        int status = param.getStatus();
        Integer minute = param.getMinute();

        ValidateUtils.notNull(id);
        ValidateUtils.notNull(status);
        ValidateUtils.valueOf(status, ImmutableList.of(0, 1));
        if (minute != null && minute > 43200) {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAMETER);
        }
        userManager.setBlock(N3d.decode(id), status, minute);

        return APIResultWrap.ok("设置/取消封号成功");
    }

    @ApiOperation(value = "获取群列表")
    @RequestMapping(value = "/group/list", method = RequestMethod.GET)
    public APIResult<Page<GroupAdminDTO>> getGroups(
            @ApiParam(name = "groupName", value = "群组名称", type = "String") @RequestParam(value = "groupName", required = false) String groupName,
            @ApiParam(name = "creatorUid", value = "群主ZBG用户id", type = "String") @RequestParam(value = "creatorUid", required = false) String creatorUid,
            @ApiParam(name = "referFlag", value = "是否推荐群0/1，不传查全部", type = "Integer") @RequestParam(value = "referFlag", required = false) Integer referFlag,
            @ApiParam(name = "hotFlag", value = "是否热聊群标志0/1，不传查全部", type = "Integer") @RequestParam(value = "hotFlag", required = false) Integer hotFlag,
            @ApiParam(name = "pageNum", value = "页码", type = "Integer") @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @ApiParam(name = "pageSize", value = "返回数据数", type = "Integer") @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    ) throws ServiceException {

        Page<GroupAdminDTO> groupList = groupManager.getGroupList(groupName, creatorUid, referFlag, hotFlag, pageNum, pageSize);

        return APIResultWrap.ok(groupList);
    }

    @ApiOperation(value = "创建群组")
    @RequestMapping(value = "/group/create", method = RequestMethod.POST)
    public APIResult<Object> create(@RequestBody GroupCreateParam groupParam) throws ServiceException {
        String name = groupParam.getName();
        String creatorUid = groupParam.getCreatorUid();
        String portraitUri = groupParam.getPortraitUri();
        Integer maxMemberCount = groupParam.getMaxMemberCount();

        ValidateUtils.checkGroupName(name);
        ValidateUtils.notEmpty(creatorUid);

        name = MiscUtils.xss(name, ValidateUtils.GROUP_NAME_MAX_LENGTH);
        ValidateUtils.checkGroupName(name);
        if (portraitUri == null) {
            portraitUri = "";
        }

        if (maxMemberCount != null && maxMemberCount > ValidateUtils.DEFAULT_MAX_GROUP_MEMBER_COUNT) {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAMETER);
        }

        Pair<Integer, String> pair = userManager.login("86", creatorUid, Users.DEFAULT_PASSWORD);

        Integer[] decodeMemberIds = new Integer[]{pair.getKey()};
        if (maxMemberCount != null && decodeMemberIds.length > maxMemberCount) {
            throw new ServiceException(ErrorCode.INVALID_GROUP_MEMNBER_MAX_COUNT);
        }

        Map<String, Object> extraMap = JSONObject.parseObject(JSON.toJSONString(groupParam));

        GroupAddStatusDTO groupAddStatusDTO = groupManager.createGroup(pair.getKey(), name, decodeMemberIds, portraitUri, extraMap);

        return APIResultWrap.ok(groupAddStatusDTO);
    }

    @ApiOperation(value = "修改群组")
    @RequestMapping(value = "/group/update", method = RequestMethod.POST)
    public APIResult<Object> update(@RequestBody GroupUpdateParam groupParam) throws ServiceException {
        String name = groupParam.getName();

        ValidateUtils.notNull(groupParam.getGroupId());
        ValidateUtils.checkGroupName(name);

        name = MiscUtils.xss(name, ValidateUtils.GROUP_NAME_MAX_LENGTH);
        ValidateUtils.checkGroupName(name);

        groupManager.updateGroup(groupParam);

        return APIResultWrap.ok();
    }

    @ApiOperation(value = "添加群成员", notes = "入参字段：groupId, userIds，接口会先给用户创建融云账户")
    @RequestMapping(value = "/group/add", method = RequestMethod.POST)
    public APIResult<Object> addMember(@RequestBody GroupAdminParam groupParam) throws ServiceException {
        String groupId = groupParam.getGroupId();
        String[] userIds = groupParam.getUserId();

        ValidateUtils.notEmpty(groupId);
        ValidateUtils.notEmpty(userIds);
        ValidateUtils.checkMemberIds(userIds);

        List<Integer> ids = userManager.batchGetOrRegisterUsers(userIds);
        if (ids.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }
        Integer[] decodeMemberIds = ids.toArray(new Integer[0]);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));
        Integer currentUserId = groupInfo.getCreatorId();

        List<UserStatusDTO> userStatusDTOList = groupManager.addMember(currentUserId, N3d.decode(groupId), decodeMemberIds);

        return APIResultWrap.ok(userStatusDTOList);
    }

    @ApiOperation(value = "退出群组", notes = "入参字段：groupId, memberId")
    @RequestMapping(value = "/group/quit", method = RequestMethod.POST)
    public APIResult<?> quitGroup(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();
        String currentUserId = groupParam.getMemberId();
        ValidateUtils.notEmpty(groupId);
        ValidateUtils.notEmpty(currentUserId);

        String resultMessage = groupManager.quitGroup(N3d.decode(currentUserId), N3d.decode(groupId), groupId);
        return APIResultWrap.ok(null, resultMessage);
    }


    @ApiOperation(value = "解散群组")
    @RequestMapping(value = "/group/dismiss", method = RequestMethod.POST)
    public APIResult<Object> dismiss(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();

        ValidateUtils.notEmpty(groupId);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));

        groupManager.dismiss(groupInfo.getCreatorId(), N3d.decode(groupId), groupId);
        return APIResultWrap.ok();
    }

    @ApiOperation(value = "批量增加管理员")
    @RequestMapping(value = "/group/set_manager", method = RequestMethod.POST)
    public APIResult<Object> batchSetManager(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();
        String[] memberIds = groupParam.getMemberIds();
        ValidateUtils.notEmpty(groupId);
        ValidateUtils.notEmpty(memberIds);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));

        groupManager.batchSetManager(groupInfo.getCreatorId(), N3d.decode(groupId), MiscUtils.decodeIds(memberIds), memberIds);
        return APIResultWrap.ok();
    }

    @ApiOperation(value = "批量删除管理员")
    @RequestMapping(value = "/group/remove_manager", method = RequestMethod.POST)
    public APIResult<Object> batchRemoveManager(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();
        String[] memberIds = groupParam.getMemberIds();

        ValidateUtils.notEmpty(groupId);
        ValidateUtils.notEmpty(memberIds);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));

        groupManager.batchRemoveManager(groupInfo.getCreatorId(), N3d.decode(groupId), MiscUtils.decodeIds(memberIds), memberIds);

        return APIResultWrap.ok();
    }

    @ApiOperation(value = "发布群公告")
    @RequestMapping(value = "/group/set_bulletin", method = RequestMethod.POST)
    public APIResult<Object> setBulletin(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();

        if (groupId == null || StringUtils.isEmpty(groupId.trim())) {
            throw new ServiceException(ErrorCode.GROUPID_NULL);
        }
        String bulletin = groupParam.getBulletin();

        ValidateUtils.notNull(bulletin);

        bulletin = MiscUtils.xss(bulletin, ValidateUtils.GROUP_BULLETIN_MAX_LENGTH);
        ValidateUtils.checkGroupBulletion(bulletin);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));
        Integer currentUserId = groupInfo.getCreatorId();
        groupManager.setBulletin(currentUserId, N3d.decode(groupId), bulletin);

        return APIResultWrap.ok();
    }

    @ApiOperation(value = "获取群公告")
    @RequestMapping(value = "/group/get_bulletin", method = RequestMethod.GET)
    public APIResult<GroupBulletinsDTO> getBulletin(
            @ApiParam(name = "groupId", value = "群组ID", required = true, type = "String", example = "86")
            @RequestParam String groupId) throws ServiceException {

        ValidateUtils.notEmpty(groupId);

        GroupBulletins groupBulletins = groupManager.getBulletin(N3d.decode(groupId));
        if (groupBulletins == null) {
            throw new ServiceException(ErrorCode.NO_GROUP_BULLETIN);
        }

        GroupBulletinsDTO groupBulletinsDTO = new GroupBulletinsDTO();

        // 返回给前端的结构id属性需要N3d编码
        groupBulletinsDTO.setGroupId(N3d.encode(groupBulletins.getGroupId()));
        groupBulletinsDTO.setContent(groupBulletins.getContent());
        groupBulletinsDTO.setId(N3d.encode(groupBulletins.getId()));
        groupBulletinsDTO.setTimestamp(groupBulletins.getTimestamp());

        return APIResultWrap.ok(groupBulletinsDTO);
    }


    @ApiOperation(value = "获取群信息")
    @RequestMapping(value = "/group/{id}", method = RequestMethod.GET)
    public APIResult<GroupDTO> getGroupInfo(
            @ApiParam(name = "id", value = "群组ID", required = true, type = "String", example = "86")
            @PathVariable("id") String groupId) throws ServiceException {
        ValidateUtils.notEmpty(groupId);

        Groups group = groupManager.getGroupInfo(N3d.decode(groupId));

        GroupDTO groupDTO = GroupDTO.copyOf(group);
        return APIResultWrap.ok(groupDTO);
    }

    @ApiOperation(value = "获取群成员列表")
    @RequestMapping(value = "/group/{id}/members", method = RequestMethod.GET)
    public APIResult<List<MemberDTO>> getGroupMembers(
            @ApiParam(name = "id", value = "群组ID", required = true, type = "String", example = "86")
            @PathVariable("id") String groupId) throws ServiceException {

        ValidateUtils.notEmpty(groupId);

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));
        Integer currentUserId = groupInfo.getCreatorId();

        List<GroupMembers> groupMembersList = groupManager.getGroupMembers(currentUserId, N3d.decode(groupId));

        List<MemberDTO> memberDTOList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMATR_PATTERN);

        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                MemberDTO memberDTO = new MemberDTO();
                memberDTO.setGroupNickname(groupMembers.getGroupNickname());
                memberDTO.setRole(groupMembers.getRole());
                memberDTO.setCreatedAt(sdf.format(groupMembers.getCreatedAt()));
                memberDTO.setCreatedTime(groupMembers.getCreatedAt().getTime());
                memberDTO.setUpdatedAt(sdf.format(groupMembers.getUpdatedAt()));
                memberDTO.setUpdatedTime(groupMembers.getUpdatedAt().getTime());

                UserDTO userDTO = new UserDTO();
                memberDTO.setUser(userDTO);

                Users u = groupMembers.getUsers();
                if (u != null) {
                    userDTO.setId(N3d.encode(u.getId()));
                    userDTO.setNickname(u.getNickname());
                    userDTO.setRegion(u.getRegion());
                    userDTO.setPhone(u.getPhone());
                    userDTO.setGender(u.getGender());
                    userDTO.setPortraitUri(u.getPortraitUri());
                    userDTO.setStAccount(u.getStAccount());

                }
                memberDTOList.add(memberDTO);
            }
        }

        return APIResultWrap.ok(memberDTOList);
    }

    @ApiOperation(value = "设置/取消 全员禁言")
    @RequestMapping(value = "/group/mute_all", method = RequestMethod.POST)
    public APIResult<Object> setMuteAll(@RequestBody GroupAdminParam groupParam) throws ServiceException {

        String groupId = groupParam.getGroupId();
        Integer muteStatus = groupParam.getMuteStatus();
        String[] userId = groupParam.getMemberIds();

        ValidateUtils.notEmpty(groupId);
        ValidateUtils.notNull(muteStatus);
        ValidateUtils.valueOf(muteStatus, ImmutableList.of(0, 1));

        Groups groupInfo = groupManager.getGroupInfo(N3d.decode(groupId));
        Integer currentUserId = groupInfo.getCreatorId();

        groupManager.setMuteAll(currentUserId, N3d.decode(groupId), muteStatus, MiscUtils.decodeIds(userId));

        return APIResultWrap.ok("全员禁言成功");
    }

}
