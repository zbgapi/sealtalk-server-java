package com.rcloud.server.sealtalk.manager;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ConversationType;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.domain.*;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.rongcloud.message.CustomerConNtfMessage;
import com.rcloud.server.sealtalk.service.*;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import io.rong.messages.TxtMessage;
import io.rong.models.Result;
import io.rong.models.message.GroupMessage;
import io.rong.models.message.PrivateMessage;
import io.rong.models.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/11
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Service
@Slf4j
public class MiscManager extends BaseManager {

    @Resource
    private RongCloudClient rongCloudClient;

    @Resource
    private FriendshipsService friendshipsService;

    @Resource
    private GroupMembersService groupMembersService;

    @Resource
    private UsersService usersService;

    @Resource
    private ScreenStatusesService screenStatusesService;

    @Resource
    private SystemNotificationService systemNotificationService;

    /**
     * 调用Server api发送消息
     *
     * @param conversationType
     * @param targetId
     * @param objectName
     * @param content
     * @param pushContent
     * @param encodedTargetId
     */
    public void sendMessage(Integer currentUserId, String conversationType, Integer targetId, String objectName, String content, String pushContent, String encodedTargetId) throws ServiceException {
        if (Constants.CONVERSATION_TYPE_PRIVATE.equals(conversationType)) {
            //如果会话类型是单聊
            Example example = new Example(Friendships.class);
            example.createCriteria().andEqualTo("userId", currentUserId)
                    .andEqualTo("friendId", targetId)
                    .andEqualTo("status", Friendships.FRIENDSHIP_AGREED);
            Friendships friendships = friendshipsService.getOneByExample(example);

            if (friendships != null) {
                //调用融云接口发送单聊消息
                PrivateMessage privateMessage = new PrivateMessage()
                        .setSenderId(N3d.encode(currentUserId))
                        .setTargetId(new String[]{encodedTargetId})
                        .setObjectName(objectName)
                        .setContent(new TxtMessage(content, ""))
                        .setPushContent(pushContent);
                rongCloudClient.sendPrivateMessage(privateMessage);
                return;
            } else {
                throw new ServiceException(ErrorCode.NOT_YOUR_FRIEND);
            }
        } else if (Constants.CONVERSATION_TYPE_GROUP.equals(conversationType)) {
            //如果会话类型是群组
            Example example = new Example(GroupMembers.class);
            example.createCriteria().andEqualTo("groupId", targetId)
                    .andEqualTo("memberId", currentUserId);
            GroupMembers groupMembers = groupMembersService.getOneByExample(example);

            if (groupMembers != null) {
                GroupMessage groupMessage = new GroupMessage();
                groupMessage.setSenderId(N3d.encode(currentUserId))
                        .setTargetId(new String[]{encodedTargetId})
                        .setObjectName(objectName)
                        .setContent(new TxtMessage(content, ""))
                        .setPushContent(pushContent);
                //发送群组消息
                rongCloudClient.sendGroupMessage(groupMessage);
            } else {

                throw new ServiceException(ErrorCode.NOT_YOUR_GROUP.getErrorCode(), "Your are not member of Group " + encodedTargetId + ".", ErrorCode.NOT_YOUR_GROUP.getErrorCode());
            }
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_CONVERSATION_TYPE);
        }
    }

    /**
     * 设置截屏通知状态
     *
     * @param currentUserId
     * @param targetId
     * @param conversationType
     * @param noticeStatus
     */
    public void setScreenCapture(Integer currentUserId, Integer targetId, Integer conversationType, Integer noticeStatus) throws ServiceException {

        String operateId = String.valueOf(targetId);
        String statusContent = noticeStatus == 0 ? "closeScreenNtf" : "openScreenNtf";
        if (conversationType == 1) {
            operateId = currentUserId < targetId ? currentUserId + "_" + targetId : targetId + "_" + currentUserId;
        }

        Users users = usersService.getByPrimaryKey(currentUserId);
        if (users != null) {
            Example example = new Example(ScreenStatuses.class);
            example.createCriteria().andEqualTo("conversationType", conversationType)
                    .andEqualTo("operateId", operateId);

            ScreenStatuses screenStatuses = screenStatusesService.getOneByExample(example);

            if (screenStatuses != null) {
                //如果存在截屏设置记录则更新状态
                screenStatuses.setStatus(noticeStatus);
                screenStatusesService.updateByPrimaryKeySelective(screenStatuses);
                //发送截屏消息
                sendScreenMsg0(currentUserId, targetId, conversationType, statusContent);
            } else {
                //如果不存在，则创建
                ScreenStatuses ss = new ScreenStatuses();
                ss.setOperateId(operateId);
                ss.setConversationType(conversationType);
                ss.setStatus(noticeStatus);
                ss.setCreatedAt(new Date());
                ss.setUpdatedAt(ss.getCreatedAt());
                screenStatusesService.saveSelective(ss);
                //发送截屏通知消息
                sendScreenMsg0(currentUserId, targetId, conversationType, statusContent);
            }
        } else {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAMETER);
        }


    }

    /**
     * 发送截屏通知消息
     *
     * @param currentUserId
     * @param targetId
     * @param conversationType
     * @param operation
     */

    private void sendScreenMsg0(Integer currentUserId, Integer targetId, Integer conversationType, String operation) throws ServiceException {

        if (ConversationType.PRIVATE.getCode().equals(conversationType)) {
            String encodeUserId = N3d.encode(currentUserId);
            String encodeTargetId = N3d.encode(targetId);

            CustomerConNtfMessage customerConNtfMessage = new CustomerConNtfMessage();
            customerConNtfMessage.setOperatorUserId(encodeUserId);
            customerConNtfMessage.setOperation(operation);

            PrivateMessage privateMessage = new PrivateMessage()
                    .setSenderId(encodeUserId)
                    .setTargetId(new String[]{encodeTargetId})
                    .setObjectName(customerConNtfMessage.getType())
                    .setContent(customerConNtfMessage);

            rongCloudClient.sendPrivateMessage(privateMessage);
        } else if (ConversationType.GROUP.getCode().equals(conversationType)) {
            rongCloudClient.sendCustomerConNtfMessage(N3d.encode(currentUserId), N3d.encode(targetId), operation);
        } else {
            throw new ServiceException(ErrorCode.REQUEST_ERROR);
        }

    }

    /**
     * 获取截屏通知状态
     *
     * @param currentUserId
     * @param targetId
     * @param conversationType
     * @return
     */
    public ScreenStatuses getScreenCapture(Integer currentUserId, Integer targetId, Integer conversationType) throws ServiceException {
        String operateId = String.valueOf(targetId);
        if (conversationType == 1) {
            operateId = currentUserId < targetId ? currentUserId + "_" + targetId : targetId + "_" + currentUserId;
        }

        Example example = new Example(ScreenStatuses.class);
        example.createCriteria().andEqualTo("operateId", operateId)
                .andEqualTo("conversationType", conversationType);
        return screenStatusesService.getOneByExample(example);
    }


    /**
     * 发送系统消息
     *
     * @param currentUserId
     * @param targetId
     * @param conversationType
     */
    public void sendScreenCaptureMsg(Integer currentUserId, Integer targetId, Integer conversationType) throws ServiceException {
        sendScreenMsg0(currentUserId, targetId, conversationType, "sendScreenNtf");
    }

    public void sendSystemMessage(String senderId, String content, String extra, Integer... decodeMemberIds) throws ServiceException {
        if (decodeMemberIds.length > 100) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }

        TxtMessage txtMessage = new TxtMessage(content, extra);

        String[] encodeIds = MiscUtils.encodeIds(decodeMemberIds);
        SystemMessage message = new SystemMessage()
                .setSenderId(StringUtils.isEmpty(senderId) ? Constants.SYSTEM_MESSAGE_FROM_USER_ID : senderId)
                .setTargetId(encodeIds)
                .setObjectName(txtMessage.getType())
                .setContent(txtMessage);
        Result result = rongCloudClient.sendSystemMessage(message);
        if (!Constants.CODE_OK.equals(result.getCode())) {
            throw new ServiceException(ErrorCode.SERVER_ERROR, "'RongCloud Server API Error Code: " + result.getCode());
        }

        saveSystemMessage(content, decodeMemberIds);
    }

    private void saveSystemMessage(String content, Integer[] memberIds) {
        long serialNo = System.currentTimeMillis();
        List<SystemNotification> notificationList = new ArrayList<>(memberIds.length);

        for (Integer memberId : memberIds) {
            SystemNotification notification = new SystemNotification();
            notification.setSerialNo(serialNo);
            notification.setMemberId(memberId);
            notification.setContent(content);
            notification.setCreatedAt(new Date());
            notification.setUpdatedAt(new Date());

            notificationList.add(notification);
        }

        systemNotificationService.batchInsert(notificationList);
    }

    public Page<SystemNotification> getSystemMessageList(String userId, String startTime, String endTime, Integer pageNum, Integer pageSize) throws ServiceException {

        Integer memberId = null;
        if (!StringUtils.isEmpty(userId)) {
            try {
                Users user = usersService.getUser(userId);
                memberId = user.getId();
            } catch (ServiceException e) {
                if (e.getErrorCode() == ErrorCode.USER_NOT_EXIST.getErrorCode()) {
                    return new Page<>(pageNum, pageSize);
                }
                throw e;
            }
        }
        Page<SystemNotification> page = PageHelper.startPage(pageNum, pageSize);
        systemNotificationService.querySystemMessageWithUserIdAndTime(memberId, startTime, endTime);
        return page;
    }
}
