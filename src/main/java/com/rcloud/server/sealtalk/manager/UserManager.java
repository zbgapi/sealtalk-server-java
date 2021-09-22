package com.rcloud.server.sealtalk.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.ImmutableList;
import com.qiniu.util.Auth;
import com.rcloud.server.sealtalk.configuration.ProfileConfig;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.SmsServiceType;
import com.rcloud.server.sealtalk.controller.param.UserListParam;
import com.rcloud.server.sealtalk.domain.*;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.exchange.domain.EcUser;
import com.rcloud.server.sealtalk.exchange.service.EcUserService;
import com.rcloud.server.sealtalk.model.ServerApiParams;
import com.rcloud.server.sealtalk.model.dto.sync.*;
import com.rcloud.server.sealtalk.rongcloud.RongCloudClient;
import com.rcloud.server.sealtalk.service.*;
import com.rcloud.server.sealtalk.sms.SmsService;
import com.rcloud.server.sealtalk.sms.SmsServiceFactory;
import com.rcloud.server.sealtalk.spi.verifycode.VerifyCodeAuthentication;
import com.rcloud.server.sealtalk.spi.verifycode.VerifyCodeAuthenticationFactory;
import com.rcloud.server.sealtalk.util.*;
import io.micrometer.core.instrument.util.IOUtils;
import io.rong.models.BlockUsers;
import io.rong.models.Result;
import io.rong.models.response.BlackListResult;
import io.rong.models.response.BlockUserResult;
import io.rong.models.response.TokenResult;
import io.rong.models.user.UserModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: xiuwei.nie
 * @Author: Jianlu.Yu
 * @Date: 2020/7/7
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Service
@Slf4j
public class UserManager extends BaseManager {

    @Resource
    private ProfileConfig profileConfig;

    @Resource
    private RongCloudClient rongCloudClient;

    @Resource
    private VerificationCodesService verificationCodesService;

    @Resource
    private VerificationViolationsService verificationViolationsService;

    @Resource
    private UsersService usersService;

    @Resource
    private DataVersionsService dataVersionsService;

    @Resource
    private GroupMembersService groupMembersService;

    @Resource
    private FriendshipsService friendshipsService;

    @Resource
    private BlackListsService blackListsService;

    @Resource
    private GroupFavsService groupFavsService;

    @Resource
    private EcUserService ecUsersService;

    @Value("classpath:region.json")
    private org.springframework.core.io.Resource regionResource;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private HttpClient httpClient;

    /**
     * 向手机发送验证码
     */
    public void sendCode(String region, String phone, SmsServiceType smsServiceType, ServerApiParams serverApiParams) throws ServiceException {
        log.info("send code. region:[{}] phone:[{}] smsServiceType:[{}]", region, phone, smsServiceType.getCode());

        region = MiscUtils.removeRegionPrefix(region);
        ValidateUtils.checkRegion(region);
        ValidateUtils.checkCompletePhone(phone);

        String ip = serverApiParams.getRequestUriInfo().getIp();

        // 获取最近一次发送验证码记录
        VerificationCodes verificationCodes = verificationCodesService.getByRegionAndPhone(region, phone);
        if (verificationCodes != null) {
            checkLimitTime(verificationCodes);
        }

        if (SmsServiceType.YUNPIAN.equals(smsServiceType)) {
            //云片服务获取验证码，检查IP请求频率限制
            checkRequestFrequency(ip);
        }

        //保存或更新verificationCodes、 发送验证码
        upsertAndSendToSms(region, phone, smsServiceType);

        //云片服务获取验证码 刷新请求频率限制指标数据
        if (SmsServiceType.YUNPIAN.equals(smsServiceType)) {
            refreshRequestFrequency(ip);
        }
    }

    /**
     * 刷新ip请求频率
     *
     * @param ip
     */
    private void refreshRequestFrequency(String ip) {
        //更新verification_violations ip地址访问时间次和数
        VerificationViolations verificationViolations = verificationViolationsService.getByPrimaryKey(ip);
        if (verificationViolations == null) {
            verificationViolations = new VerificationViolations();
            verificationViolations.setIp(ip);
            verificationViolations.setCount(1);
            verificationViolations.setTime(new Date());
            verificationViolationsService.saveSelective(verificationViolations);
        } else {
            DateTime dateTime = new DateTime(new Date());
            dateTime = dateTime.minusHours(sealtalkConfig.getYunpianLimitedTime());
            Date limitDate = dateTime.toDate();
            if (limitDate.after(verificationViolations.getTime())) {
                //如果上次记录时间已经是1小时前，重置计数和开始时间
                verificationViolations.setCount(1);
                verificationViolations.setTime(new Date());
            } else {
                verificationViolations.setCount(verificationViolations.getCount() + 1);
            }
            verificationViolationsService.updateByPrimaryKeySelective(verificationViolations);
        }

    }

    /**
     * 发送短信并更新数据库
     */
    private VerificationCodes upsertAndSendToSms(String region, String phone, SmsServiceType smsServiceType) throws ServiceException {
        if (Constants.ENV_DEV.equals(sealtalkConfig.getConfigEnv())) {
            //开发环境直接插入数据库，不调用短信接口
            return verificationCodesService.saveOrUpdate(region, phone, "");
        } else {
            SmsService smsService = SmsServiceFactory.getSmsService(smsServiceType);
            String sessionId = smsService.sendVerificationCode(region, phone);
            return verificationCodesService.saveOrUpdate(region, phone, sessionId);
        }
    }

    /**
     * 检查发送时间限制
     * 开发环境间隔5秒后  可再次请求发送验证码
     * 生产环境间隔1分钟后 可再次请求发送验证码
     *
     * @param verificationCodes
     * @throws ServiceException
     */
    private void checkLimitTime(VerificationCodes verificationCodes)
            throws ServiceException {

        DateTime dateTime = new DateTime(new Date());
        if (Constants.ENV_DEV.equals(sealtalkConfig.getConfigEnv())) {
            dateTime = dateTime.minusSeconds(50);
        } else {
            dateTime = dateTime.minusMinutes(1);
        }
        Date limitDate = dateTime.toDate();

        if (limitDate.before(verificationCodes.getUpdatedAt())) {
            throw new ServiceException(ErrorCode.LIMIT_ERROR);
        }
    }

    /**
     * IP请求频率限制检查
     *
     * @param ip
     * @throws ServiceException
     */
    private void checkRequestFrequency(String ip) throws ServiceException {
        Integer yunpianLimitedTime = sealtalkConfig.getYunpianLimitedTime();
        Integer yunpianLimitedCount = sealtalkConfig.getYunpianLimitedCount();

        if (yunpianLimitedTime == null) {
            yunpianLimitedTime = 1;
        }

        if (yunpianLimitedCount == null) {
            yunpianLimitedCount = 20;
        }

        VerificationViolations verificationViolations = verificationViolationsService.getByPrimaryKey(ip);
        if (verificationViolations == null) {
            return;
        }

        DateTime dateTime = new DateTime(new Date());
        Date sendDate = dateTime.minusHours(yunpianLimitedTime).toDate();

        boolean beyondLimit = verificationViolations.getCount() >= yunpianLimitedCount;

        //如果上次请求发送验证码的时间在1小时内，并且次数达到阈值，返回异常"Too many times sent"
        if (sendDate.before(verificationViolations.getTime()) && beyondLimit) {
            throw new ServiceException(ErrorCode.YUN_PIAN_SMS_ERROR);
        }
    }


    /**
     * 判断用户是否已经存在
     *
     * @param region
     * @param phone
     * @return true 存在，false 不存在
     * @throws ServiceException
     */
    public boolean isExistUser(String region, String phone) throws ServiceException {
        Users param = new Users();
        param.setRegion(region);
        param.setPhone(phone);
        Users users = usersService.getOne(param);
        return users != null;
    }


    /**
     * 校验验证码
     *
     * @param region
     * @param phone
     * @param code
     * @param smsServiceType
     * @return
     * @throws ServiceException
     */
    public String verifyCode(String region, String phone, String code, SmsServiceType smsServiceType) throws ServiceException {

        VerificationCodes verificationCodes = verificationCodesService.getByRegionAndPhone(region, phone);
        VerifyCodeAuthentication verifyCodeAuthentication = VerifyCodeAuthenticationFactory.getVerifyCodeAuthentication(smsServiceType);
        verifyCodeAuthentication.validate(verificationCodes, code, sealtalkConfig.getConfigEnv());
        return verificationCodes.getToken();
    }

    public Integer register(String nickname, String password, String verificationToken) throws ServiceException {

        VerificationCodes verificationCodes = verificationCodesService.getByToken(verificationToken);

        if (verificationCodes == null) {
            throw new ServiceException(ErrorCode.UNKNOWN_VERIFICATION_TOKEN);
        }

        Users param = new Users();
        param.setRegion(verificationCodes.getRegion());
        param.setPhone(verificationCodes.getPhone());
        Users users = usersService.getOne(param);

        if (users != null) {
            throw new ServiceException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        //如果没有注册过，密码hash
        int salt = RandomUtil.randomBetween(1000, 9999);
        String hashStr = MiscUtils.hash(password, salt);

        Users u = register0(nickname, null, verificationCodes.getRegion(), verificationCodes.getPhone(), salt, hashStr);

        //缓存nickname
        CacheUtil.set(CacheUtil.NICK_NAME_CACHE_PREFIX + u.getId(), u.getNickname());

        return u.getId();
    }

    /**
     * 注册插入user 表、dataversion表
     * 同一事务
     *
     * @param nickname
     * @param region
     * @param phone
     * @param salt
     * @param hashStr
     * @return
     */
    private Users register0(String nickname, String portraitUri, String region, String phone, int salt, String hashStr) {
        return transactionTemplate.execute(new TransactionCallback<Users>() {
            @Override
            public Users doInTransaction(TransactionStatus transactionStatus) {
                //插入user表
                Users u = new Users();
                u.setNickname(nickname);
                u.setRegion(region);
                u.setPhone(phone);
                u.setPasswordHash(hashStr);
                u.setPasswordSalt(String.valueOf(salt));
                u.setCreatedAt(new Date());
                u.setUpdatedAt(u.getCreatedAt());
                u.setPortraitUri(StringUtils.isEmpty(portraitUri) ? sealtalkConfig.getRongcloudDefaultPortraitUrl() : portraitUri);

                usersService.saveSelective(u);


                //插入DataVersion表
                DataVersions dataVersions = new DataVersions();
                dataVersions.setUserId(u.getId());
                dataVersionsService.saveSelective(dataVersions);

                return u;
            }
        });

    }

    public List<Integer> batchGetOrRegisterUsers(String[] userIds) throws ServiceException {
        List<EcUser> ecUserList = ecUsersService.getUsers(Arrays.asList(userIds));
        if (ecUserList.size() != userIds.length) {
            throw new ServiceException(ErrorCode.USER_ID_INVALID);
        }
        Example example = new Example(Users.class);
        example.createCriteria().andIn("phone", ecUserList.stream().map(EcUser::getUserId).collect(Collectors.toList()));
        List<Users> usersList = usersService.getByExample(example);
        Set<String> exists = usersList.stream().map(Users::getPhone).collect(Collectors.toSet());
        List<EcUser> notRegisterUserList = ecUserList.stream().filter(e -> !exists.contains(e.getUserId())).collect(Collectors.toList());

        List<Users> insertUserList = new ArrayList<>();
        for (EcUser ecUser : notRegisterUserList) {
            Users u = new Users();
            u.setNickname(ecUser.getHiddenName());
            u.setRegion("86");
            u.setPhone(ecUser.getUserId());
            int salt = RandomUtil.randomBetween(1000, 9999);
            String hashStr = MiscUtils.hash(Users.DEFAULT_PASSWORD, salt);
            u.setPasswordHash(hashStr);
            u.setPasswordSalt(String.valueOf(salt));
            u.setCreatedAt(new Date());
            u.setUpdatedAt(u.getCreatedAt());
            u.setPortraitUri(MiscUtils.toString(ecUser.getHeadImg(), sealtalkConfig.getRongcloudDefaultPortraitUrl()));
            u.setGender(ecUser.getSex() != null && ecUser.getSex() == 0 ? "female" : "male");
            insertUserList.add(u);
        }

        List<Integer> ids = usersService.batchInsert(insertUserList);
        dataVersionsService.batchInsert(ids);

        insertUserList.addAll(usersList);
        for (Users user : insertUserList) {
            registerRongCloud(user);
        }
        return insertUserList.stream().map(Users::getId).collect(Collectors.toList());
    }

    private String registerRongCloud(Users u) throws ServiceException {
        if (StringUtils.isEmpty(u.getRongCloudToken())) {
            //如果user表中的融云token为空，调用融云sdk 获取token
            //如果用户头像地址为空，采用默认头像地址
            String portraitUri = StringUtils.isEmpty(u.getPortraitUri()) ? sealtalkConfig.getRongcloudDefaultPortraitUrl() : u.getPortraitUri();
            TokenResult tokenResult = rongCloudClient.register(N3d.encode(u.getId()), u.getNickname(), portraitUri);
            if (!Constants.CODE_OK.equals(tokenResult.getCode())) {
                throw new ServiceException(ErrorCode.SERVER_ERROR, "'RongCloud Server API Error Code: " + tokenResult.getCode());
            }

            String token = tokenResult.getToken();

            //获取后根据userId更新表中token
            Users users = new Users();
            users.setId(u.getId());
            users.setRongCloudToken(token);
            users.setUpdatedAt(new Date());
            usersService.updateByPrimaryKeySelective(users);

            return token;
        }

        return u.getRongCloudToken();
    }

    /**
     * 用户登录
     *
     * @param region
     * @param phone
     * @param password
     * @return Pair<L, R> L=用户ID，R=融云token
     * @throws ServiceException
     */
    public Pair<Integer, String> login(String region, String phone, String password) throws ServiceException {

        Users param = new Users();
        param.setRegion(region);
        param.setPhone(phone);
        Users u = usersService.getOne(param);

        //判断用户是否存在，不存在则创建一个
        if (u == null) {
            int salt = RandomUtil.randomBetween(1000, 9999);
            String hashStr = MiscUtils.hash(password, salt);

            EcUser ecUser = ecUsersService.getUser(phone);
            if (ecUser == null) {
                throw new ServiceException(ErrorCode.USER_NOT_EXIST);
            }

            u = register0(ecUser.getHiddenName(), ecUser.getHeadImg(), region, phone, salt, hashStr);
        }

        //校验密码是否正确
//        String passwordHash = MiscUtils.hash(password, Integer.parseInt(u.getPasswordSalt()));
//
//        if (passwordHash != null && !passwordHash.equals(u.getPasswordHash())) {
//            throw new ServiceException(ErrorCode.USER_PASSWORD_WRONG);
//        }

        log.info("login id:" + u.getId() + " nickname:" + u.getNickname());
        //缓存nickname
        CacheUtil.set(CacheUtil.NICK_NAME_CACHE_PREFIX + u.getId(), u.getNickname());

        //查询该用户所属的所有组,同步到融云
        List<Groups> groupsList = new ArrayList<>();
        Map<String, String> idNamePariMap = new HashMap<>();
        List<GroupMembers> groupMembersList = groupMembersService.queryGroupMembersWithGroupByMemberId(u.getId());
        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers gm : groupMembersList) {
                Groups groups = gm.getGroups();
                if (groups != null) {
                    groupsList.add(groups);
                    idNamePariMap.put(N3d.encode(groups.getId()), groups.getName());
                }
            }
        }

        //同步前记录日志
        log.info("'Sync groups: {}", idNamePariMap);

        //调用融云sdk 将登录用户的userid，与groupIdName信息同步到融云
        try {
            Result result = rongCloudClient.syncGroupInfo(N3d.encode(u.getId()), groupsList);
            if (!Constants.CODE_OK.equals(result.getCode())) {
                log.error("Error sync user's group list failed,code:" + result.getCode());
            }
        } catch (Exception e) {
            log.error("Error sync user's group list error:" + e.getMessage(), e);
        }

        String token = registerRongCloud(u);

        //返回userId、token
        return Pair.of(u.getId(), token);
    }

    /**
     * 重置密码
     *
     * @param password
     * @param verificationToken
     * @throws ServiceException
     */
    public void resetPassword(String password, String verificationToken) throws ServiceException {

        VerificationCodes verificationCodes = verificationCodesService.getByToken(verificationToken);

        if (verificationCodes == null) {
            throw new ServiceException(ErrorCode.UNKNOWN_VERIFICATION_TOKEN);
        }

        //新密码hash,修改user表密码字段
        int salt = RandomUtil.randomBetween(1000, 9999);
        String hashStr = MiscUtils.hash(password, salt);

        updatePassword(verificationCodes.getRegion(), verificationCodes.getPhone(), salt, hashStr);
    }

    /**
     * 修改密码
     *
     * @param newPassword
     * @param oldPassword
     * @param currentUserId
     * @throws ServiceException
     */
    public void changePassword(String newPassword, String oldPassword, Integer currentUserId) throws ServiceException {

        Users u = usersService.getByPrimaryKey(currentUserId);

        if (u == null) {
            throw new ServiceException(ErrorCode.REQUEST_ERROR);
        }

        String oldPasswordHash = MiscUtils.hash(oldPassword, Integer.valueOf(u.getPasswordSalt()));

        if (!oldPasswordHash.equals(u.getPasswordHash())) {
            throw new ServiceException(ErrorCode.USER_PASSWORD_WRONG_2);
        }

        //新密码hash,修改user表密码字段
        int salt = RandomUtil.randomBetween(1000, 9999);
        String hashStr = MiscUtils.hash(newPassword, salt);

        updatePassword(u.getRegion(), u.getPhone(), salt, hashStr);
    }

    private void updatePassword(String region, String phone, int salt, String hashStr) {
        Users user = new Users();
        user.setPasswordHash(hashStr);
        user.setPasswordSalt(String.valueOf(salt));
        user.setUpdatedAt(new Date());

        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("region", region);
        criteria.andEqualTo("phone", phone);
        usersService.updateByExampleSelective(user, example);
    }

    /**
     * 设置当前用户的昵称
     *
     * @param nickname
     * @param currentUserId
     * @throws ServiceException
     */
    public void setNickName(String nickname, Integer currentUserId) throws ServiceException {
        long timestamp = System.currentTimeMillis();
        //修改昵称
        Users users = new Users();
        users.setId(currentUserId);
        users.setNickname(nickname);
        users.setTimestamp(timestamp);
        users.setUpdatedAt(new Date());
        usersService.updateByPrimaryKeySelective(users);

        //调用融云刷新用户信息
        try {
            Result result = rongCloudClient.updateUser(N3d.encode(currentUserId), nickname, null);
            if (!result.getCode().equals(200)) {
                log.error("RongCloud Server API Error code: {},errorMessage: {}", result.getCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("invoke rongCloudClient updateUser exception: " + e.getMessage(), e);
        }
        //缓存用户昵称
        CacheUtil.set(CacheUtil.NICK_NAME_CACHE_PREFIX + currentUserId, nickname);

        //清空缓存、更新版本
        clearCacheAndUpdateVersion(currentUserId, timestamp);

        return;
    }

    /**
     * 设置当前用户头像
     *
     * @param portraitUri
     * @param currentUserId
     * @throws ServiceException
     */
    public void setPortraitUri(String portraitUri, Integer currentUserId) throws ServiceException {

        long timestamp = System.currentTimeMillis();
        Users u = usersService.getByPrimaryKey(currentUserId);
        if (u == null) {
            throw new ServiceException(ErrorCode.REQUEST_ERROR);
        }
        //修改头像
        Users users = new Users();
        users.setId(currentUserId);
        users.setPortraitUri(portraitUri);
        users.setTimestamp(timestamp);
        users.setUpdatedAt(new Date());
        usersService.updateByPrimaryKeySelective(users);

        //调用融云刷新用户信息
        try {
            Result result = rongCloudClient.updateUser(N3d.encode(currentUserId), u.getNickname(), portraitUri);
            if (!result.getCode().equals(200)) {
                log.error("RongCloud Server API Error code: {},errorMessage: {}", result.getCode(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("invoke rongCloudClient updateUser exception: " + e.getMessage(), e);
        }

        //清空缓存、更新版本
        clearCacheAndUpdateVersion(currentUserId, timestamp);
        return;
    }

    /**
     * 清除user相关缓存并更新dataversion版本
     *
     * @param currentUserId
     */
    private void clearCacheAndUpdateVersion(Integer currentUserId, long timestamp) {
        //修改DataVersion表中 UserVersion
        DataVersions dataVersions = new DataVersions();
        dataVersions.setUserId(currentUserId);
        dataVersions.setUserVersion(timestamp);
        dataVersionsService.updateByPrimaryKeySelective(dataVersions);

        //修改DataVersion表中 AllFriendshipVersion
        dataVersionsService.updateAllFriendshipVersion(currentUserId, timestamp);

        //清除缓存"user_" + currentUserId
        //清除缓存"friendship_profile_user_" + currentUserId
        CacheUtil.delete(CacheUtil.USER_CACHE_PREFIX + currentUserId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_PROFILE_USER_CACHE_PREFIX + currentUserId);

        //查询该用户所有好友关系,并清除缓存friendship_all_+friendId
        Friendships f = new Friendships();
        f.setUserId(currentUserId);
        List<Friendships> friendshipsList = friendshipsService.get(f);
        if (!CollectionUtils.isEmpty(friendshipsList)) {
            for (Friendships friendships : friendshipsList) {
                CacheUtil.delete(CacheUtil.FRIENDSHIP_ALL_CACHE_PREFIX + friendships.getFriendId());
            }
        }
        //查询该用户所属组groupid isDeleted: false
        //清空缓存group_members_
        Example example = new Example(GroupMembers.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("memberId", currentUserId);
        criteria.andEqualTo("isDeleted", GroupMembers.IS_DELETED_NO);

        List<GroupMembers> groupMembersList = groupMembersService.getByExample(example);
        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                CacheUtil.delete(CacheUtil.GROUP_MEMBERS_CACHE_PREFIX + groupMembers.getGroupId());
            }
        }
    }

    /**
     * 获取融云token
     * 1、根据currentUserId查询User
     * 2、调用融云用户注册接口，获取token
     * 3、根据userId更新本地数据users表中rongCloudToken
     * 4、把userid，token返回给前端
     */
    public Pair<Integer, String> getToken(Integer currentUserId) throws ServiceException {

        Users user = usersService.getByPrimaryKey(currentUserId);

        //调用融云用户注册接口，获取token
        //如果用户头像地址为空，采用默认头像地址
        String portraitUri = StringUtils.isEmpty(user.getPortraitUri()) ? sealtalkConfig.getRongcloudDefaultPortraitUrl() : user.getPortraitUri();
        TokenResult tokenResult = rongCloudClient.register(N3d.encode(user.getId()), user.getNickname(), portraitUri);
        String token = tokenResult.getToken();

        //根据userId更新本地数据users表中rongCloudToken
        Users param = new Users();
        param.setId(user.getId());
        param.setRongCloudToken(token);
        param.setUpdatedAt(new Date());
        usersService.updateByPrimaryKeySelective(param);

        return Pair.of(user.getId(), token);
    }

    /**
     * 获取黑名单列表
     * 1、从cookie中获取currentUserId
     * 2、根据currentUserId从缓存中获取黑名单列表，存在直接返回
     * 3、如果缓存不存在，查询数据库黑名单表，调用融云服务远程获取黑名单列表
     * 4、以融云服务黑名单列表为准，merge更新到数据库
     * 5、缓存到cache、返回最新的黑名单列表
     *
     * @param currentUserId
     * @return
     * @throws ServiceException
     */
    public List<BlackLists> getBlackList(Integer currentUserId) throws ServiceException {

        long timestamp = System.currentTimeMillis();
        //从缓存中获取blacklist，存在直接返回
        String blackListStr = CacheUtil.get(CacheUtil.USER_BLACKLIST_CACHE_PREFIX + currentUserId);
        if (!StringUtils.isEmpty(blackListStr)) {
            return JacksonUtil.fromJson(blackListStr, List.class, BlackLists.class);
        }
        //查询数据库blacklist表
        List<BlackLists> dbBlackLists = blackListsService.getBlackListsWithFriendUsers(currentUserId);

        //调用融云服务接口获取黑名单
        BlackListResult blackListResult = rongCloudClient.queryUserBlackList(N3d.encode(currentUserId));

        UserModel[] serverBlackList = blackListResult.getUsers();

        List<Long> serverBlackListIds = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(serverBlackList)) {
            for (UserModel userModel : serverBlackList) {
                long id = N3d.decode(userModel.getId());
                serverBlackListIds.add(id);
            }
        }

        List<Long> dbBlacklistUserIds = new ArrayList<>();

        boolean hasDirtyData = false;
        if (!CollectionUtils.isEmpty(dbBlackLists)) {
            for (BlackLists blackLists : dbBlackLists) {
                if (blackLists.getUsers() != null) {
                    Long userId = Long.valueOf(blackLists.getUsers().getId());
                    dbBlacklistUserIds.add(userId);
                } else {
                    hasDirtyData = true;
                }
            }
        }
        if (hasDirtyData) {
            log.error("Dirty blacklist data currentUserId:{}", currentUserId);
        }

        //比对远程接口获取到的黑名单列表和数据库本地表数据
        //如果远程存在，本地不存在，插入本地数据表
        if (!CollectionUtils.isEmpty(serverBlackListIds)) {
            for (Long serverId : serverBlackListIds) {
                //需要每条数据都更新数据库吗？TODO
                if (!dbBlacklistUserIds.contains(serverId)) {
                    blackListsService.saveOrUpdate(currentUserId, serverId.intValue(), BlackLists.STATUS_VALID, timestamp);
                    log.info("Sync: fix user blacklist, add {} -> {} from db.", currentUserId, serverId);
                    //刷新黑名单版本
                    dataVersionsService.updateBlacklistVersion(currentUserId, timestamp);
                }
            }
        }

        //如果远程不存在，本地存在，逻辑删除本地数据表数据
        if (!CollectionUtils.isEmpty(dbBlacklistUserIds)) {
            for (Long userId : dbBlacklistUserIds) {
                if (!serverBlackListIds.contains(userId)) {

                    blackListsService.updateStatus(currentUserId, userId.intValue(), BlackLists.STATUS_INVALID, timestamp);
                    log.info("Sync: fix user blacklist, remove {} -> {} from db.", currentUserId, userId);

                    //刷新黑名单版本
                    dataVersionsService.updateBlacklistVersion(currentUserId, timestamp);
                }
            }
        }

        //缓存结果
        CacheUtil.set(CacheUtil.USER_BLACKLIST_CACHE_PREFIX + currentUserId, JacksonUtil.toJson(dbBlackLists));

        return dbBlackLists;
    }


    /**
     * 将好友加入黑名单
     * 1、检查参数，好友ID是否存在用户表中，不存在返回404，friendId is not an available userId.
     * 2、存在则调用融云服务接口新增黑名单
     * 3、将黑名单信息插入或更新本地数据库，然后更新黑名单版本
     * 4、然后清除缓存"user_blacklist_" + currentUserId
     * 5、更新Friendship 表状态信息为黑名单状态 FRIENDSHIP_BLACK = 31
     * 6、然后清除friendship相关缓存
     * -》Cache.del("friendship_profile_displayName_" + currentUserId + "_" + friendId);
     * -》Cache.del("friendship_profile_user_" + currentUserId + "_" + friendId);
     * -》Cache.del("friendship_all_" + currentUserId);
     * -》Cache.del("friendship_all_" + friendId);
     */
    public void addBlackList(Integer currentUserId, Integer friendId, String encodedFriendId) throws ServiceException {

        long timestamp = System.currentTimeMillis();
        //判断friendId 用户是否存在
        Users user = usersService.getByPrimaryKey(friendId);
        if (user == null) {
            throw new ServiceException(ErrorCode.FRIEND_USER_NOT_EXIST);
        }

        String[] blackFriendIds = {encodedFriendId};
        //调用融云服务接口新增黑名单
        rongCloudClient.addUserBlackList(N3d.encode(currentUserId), blackFriendIds);

        //将黑名单信息插入或更新本地数据库
        blackListsService.saveOrUpdate(currentUserId, friendId, BlackLists.STATUS_VALID, timestamp);

        //更新黑名单数据版本
        dataVersionsService.updateBlacklistVersion(currentUserId, timestamp);

        //清除user_blacklist_缓存
        CacheUtil.delete(CacheUtil.USER_BLACKLIST_CACHE_PREFIX + currentUserId);

        //更新好友关系状态为黑名单状态
        friendshipsService.updateFriendShipBlacklistsStatus(currentUserId, friendId);

        //清除friendship相关缓存
        CacheUtil.delete(CacheUtil.FRIENDSHIP_PROFILE_CACHE_PREFIX + currentUserId + "_" + friendId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_PROFILE_USER_CACHE_PREFIX + currentUserId + "_" + friendId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_ALL_CACHE_PREFIX + currentUserId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_ALL_CACHE_PREFIX + friendId);

        return;
    }

    /**
     * 将好友移除黑名单
     * 1、调用融云服务接口移除黑名单
     * 2、更新本地Blacklist 表，设置记录状态status为false
     * 3、然后更新DataVersion表BlacklistVersion 版本
     * 4、清除缓存user_blacklist_
     * 5、更新Friendship 表状态信息为 FRIENDSHIP_AGREED = 20
     * 4、然后清除相关缓存
     * -》Cache.del("friendship_profile_displayName_" + currentUserId + "_" + friendId);
     * -》Cache.del("friendship_profile_user_" + currentUserId + "_" + friendId);
     * -》Cache.del("friendship_all_" + currentUserId);
     * -》 Cache.del("friendship_all_" + friendId);
     *
     * @param currentUserId
     * @param friendId
     * @param encodedFriendId
     * @throws ServiceException
     */
    public void removeBlackList(Integer currentUserId, Integer friendId, String encodedFriendId) throws ServiceException {

        long timestamp = System.currentTimeMillis();

        String[] blackFriendIds = {encodedFriendId};
        //调用融云服务接口移除黑名单
        rongCloudClient.removeUserBlackList(N3d.encode(currentUserId), blackFriendIds);

        //更新本地Blacklist 表，设置记录状态status为false
        blackListsService.updateStatus(currentUserId, friendId, BlackLists.STATUS_INVALID, timestamp);

        //刷新黑名单数据版本
        dataVersionsService.updateBlacklistVersion(currentUserId, timestamp);

        //清除缓存user_blacklist_
        CacheUtil.delete(CacheUtil.USER_BLACKLIST_CACHE_PREFIX + currentUserId);

        //更新Friendship 表状态信息为 FRIENDSHIP_AGREED = 20
        friendshipsService.updateAgreeStatus(currentUserId, friendId, timestamp, ImmutableList.of(Friendships.FRIENDSHIP_PULLEDBLACK));
        log.info("result--remove db black currentUserId={},friendId={}", currentUserId, friendId);

        //清除friendship相关缓存
        CacheUtil.delete(CacheUtil.FRIENDSHIP_PROFILE_CACHE_PREFIX + currentUserId + "_" + friendId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_PROFILE_USER_CACHE_PREFIX + currentUserId + "_" + friendId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_ALL_CACHE_PREFIX + currentUserId);
        CacheUtil.delete(CacheUtil.FRIENDSHIP_ALL_CACHE_PREFIX + friendId);
        log.info("result--remove cache black currentUserId={},friendId={}", currentUserId, friendId);

        return;
    }

    /**
     * 获取用户所属群组
     * -》先从缓存中获取，缓存中不存在查询db获取
     *
     * @param currentUserId
     * @return
     */
    public List<Groups> getGroups(Integer currentUserId) throws ServiceException {

        List<Groups> groupsList = new ArrayList<>();

        String groupsJson = CacheUtil.get(CacheUtil.USER_GROUP_CACHE_PREFIX + currentUserId);

        if (!StringUtils.isEmpty(groupsJson)) {
            return JacksonUtil.fromJson(groupsJson, List.class, Groups.class);
        }

        //缓存中为空，去查询db
        List<GroupMembers> groupMembersList = groupMembersService.queryGroupMembersWithGroupByMemberId(currentUserId);

        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                groupsList.add(groupMembers.getGroups());
            }
        }
        CacheUtil.set(CacheUtil.USER_GROUP_CACHE_PREFIX + currentUserId, JacksonUtil.toJson(groupsList));
        return groupsList;
    }

    /**
     * 根据id查询用户信息
     *
     * @param userId
     * @return
     */
    public Users getUser(Integer userId) {
        return usersService.getByPrimaryKey(userId);
    }

    /**
     * 根据userIds批量查询用户信息
     *
     * @param userIds
     * @return
     */
    public List<Users> getBatchUser(List<Integer> userIds) {

        Example example = new Example(Users.class);
        example.createCriteria().andIn("id", userIds);
        return usersService.getByExample(example);
    }

    /**
     * 根据stAccount查询用户信息
     *
     * @param stAccount
     * @return
     */
    public Users getUserByStAccount(String stAccount) {
        Users u = new Users();
        u.setStAccount(stAccount);
        return usersService.getOne(u);
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param region
     * @param phone
     * @return
     */
    public Users getUser(String region, String phone) {
        Users u = new Users();
        u.setRegion(region);
        u.setPhone(phone);
        return usersService.getOne(u);
    }

    /**
     * 设置用户信息
     *
     * @param u
     */
    public void updateUserById(Users u) {
        usersService.updateByPrimaryKeySelective(u);
    }

    /**
     * 设置 SealTlk 号
     *
     * @param currentUserId
     * @param stAccount
     * @throws ServiceException
     */
    public void setStAccount(Integer currentUserId, String stAccount) throws ServiceException {
        Users u = new Users();
        u.setStAccount(stAccount);

        Users users = usersService.getOne(u);
        if (users != null) {
            throw new ServiceException(ErrorCode.USER_STACCOUNT_EXIST);
        }

        u.setId(currentUserId);
        usersService.updateByPrimaryKeySelective(u);

    }

    /**
     * 获取通讯录群组列表
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public Pair<Integer, List<Groups>> getFavGroups(Integer userId, Integer limit, Integer offset) throws ServiceException {
        List<Groups> groupsList = new ArrayList<>();
        Integer count = groupFavsService.queryCountGroupFavs(userId);
        if (count != null && count > 0) {
            List<GroupFavs> groupFavsList = groupFavsService.queryGroupFavsWithGroupByUserId(userId, limit, offset);
            if (!CollectionUtils.isEmpty(groupFavsList)) {
                for (GroupFavs groupFavs : groupFavsList) {
                    if (groupFavs.getGroups() != null) {
                        groupsList.add(groupFavs.getGroups());
                    }
                }
            }
        }

        return Pair.of(count, groupsList);
    }

    /**
     * 获取区域信息
     *
     * @return
     * @throws ServiceException
     */
    public JsonNode getRegionList() throws ServiceException {
        try {
            String regionData = CacheUtil.get(CacheUtil.REGION_LIST_DATA);
            if (!StringUtils.isEmpty(regionData)) {
                return JacksonUtil.getJsonNode(regionData);
            }
            regionData = IOUtils
                    .toString(regionResource.getInputStream(), StandardCharsets.UTF_8);
            CacheUtil.set(CacheUtil.REGION_LIST_DATA, regionData);
            return JacksonUtil.getJsonNode(regionData);
        } catch (Exception e) {
            log.error("get Region resource error:" + e.getMessage(), e);
            throw new ServiceException(ErrorCode.INVALID_REGION_LIST);
        }

    }

    /**
     * 获取云存储token
     */
    public String getImageToken() {

        String accessKey = sealtalkConfig.getQiniuAccessKey();
        String secretKey = sealtalkConfig.getQiniuSecretKey();
        String bucket = sealtalkConfig.getQiniuBucketName();
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        return upToken;
    }

    /**
     * 获取短信图形验证码
     */
    public String getSmsImgCode() {

        //TODO 后面改为调用sdk
        String result = httpClient.get(Constants.URL_GET_RONGCLOUD_IMG_CODE + sealtalkConfig.getRongcloudAppKey());
        return result;
    }


    /**
     * 同步用户的好友、黑名单、群组、群组成员数据
     *
     * @param currentUserId
     * @param version
     */
    public SyncInfoDTO getSyncInfo(Integer currentUserId, Long version) throws ServiceException {

        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();

        //获取用户的各数据版本 userVersion、blacklistVersion、friendshipVersion、groupVersion、groupMemberVersion
        DataVersions dataVersions = dataVersionsService.getByPrimaryKey(currentUserId);

        Users users = null;
        List<BlackLists> blackListsList = new ArrayList<>();
        List<Friendships> friendshipsList = new ArrayList<>();
        List<GroupMembers> groupsList = new ArrayList<>();
        List<GroupMembers> groupMembersList = new ArrayList<>();

        if (dataVersions.getUserVersion() > version) {
            //获取用户信息
            users = usersService.getByPrimaryKey(currentUserId);
        }

        if (dataVersions.getBlacklistVersion() > version) {
            //获取用户黑名单信息
            blackListsList = blackListsService.getBlackListsWithFriendUsers(currentUserId, version);
        }

        if (dataVersions.getFriendshipVersion() > version) {
            friendshipsList = friendshipsService.getFriendShipListWithUsers(currentUserId, version);
        }

        List<Integer> groupIdList = new ArrayList<>();
        if (dataVersions.getGroupVersion() > version) {
            groupsList = groupMembersService.queryGroupMembersWithGroupByMemberId(currentUserId);
            if (!CollectionUtils.isEmpty(groupsList)) {
                for (GroupMembers groupMember : groupsList) {
                    if (groupMember.getGroups() != null) {
                        groupIdList.add(groupMember.getGroups().getId());
                    }
                }
            }
        }

        if (dataVersions.getGroupVersion() > version) {

            groupMembersList = groupMembersService.queryGroupMembersWithUsersByGroupIdsAndVersion(groupIdList, version);
        }

        Long maxVersion = 0L;
        if (users != null) {
            maxVersion = users.getTimestamp();
        }

        if (blackListsList != null) {
            for (BlackLists blackLists : blackListsList) {
                if (blackLists.getTimestamp() > maxVersion) {
                    maxVersion = blackLists.getTimestamp();
                }
            }
        }

        if (friendshipsList != null) {
            for (Friendships friendships : friendshipsList) {
                if (friendships.getTimestamp() > maxVersion) {
                    maxVersion = friendships.getTimestamp();
                }
            }
        }

        if (groupsList != null) {
            for (GroupMembers groupMembers : groupsList) {
                if (groupMembers.getGroups() != null) {
                    if (groupMembers.getGroups().getTimestamp() > maxVersion) {
                        maxVersion = groupMembers.getGroups().getTimestamp();
                    }
                }

            }
        }

        if (groupMembersList != null) {
            for (GroupMembers groupMembers : groupMembersList) {
                if (groupMembers.getTimestamp() > maxVersion) {
                    maxVersion = groupMembers.getTimestamp();
                }
            }
        }

        log.info("sync info ,maxVersion={}", maxVersion);

        syncInfoDTO.setVersion(version);


        SyncUserDTO userDTO = new SyncUserDTO();
        if (users != null) {
            userDTO.setId(N3d.encode(users.getId()));
            userDTO.setNickname(users.getNickname());
            userDTO.setPortraitUri(users.getPortraitUri());
            userDTO.setTimestamp(users.getTimestamp());
        }
        syncInfoDTO.setUser(userDTO);

        List<SyncBlackListDTO> syncBlackListDTOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(blackListsList)) {
            for (BlackLists blackLists : blackListsList) {
                SyncBlackListDTO syncBlackListDTO = new SyncBlackListDTO();
                syncBlackListDTO.setFriendId(N3d.encode(blackLists.getFriendId()));
                syncBlackListDTO.setStatus(BlackLists.STATUS_VALID.equals(blackLists.getStatus()) ? true : false);
                syncBlackListDTO.setTimestamp(blackLists.getTimestamp());

                Users u = blackLists.getUsers();
                SyncUserDTO sUser = new SyncUserDTO();

                if (u != null) {
                    sUser.setId(N3d.encode(u.getId()));
                    sUser.setNickname(u.getNickname());
                    sUser.setPortraitUri(u.getPortraitUri());
                    sUser.setTimestamp(u.getTimestamp());
                }
                syncBlackListDTO.setUser(sUser);
                syncBlackListDTOList.add(syncBlackListDTO);
            }
        }

        syncInfoDTO.setBlacklist(syncBlackListDTOList);


        List<SyncFriendshipDTO> syncFriendshipDTOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(friendshipsList)) {
            for (Friendships friendships : friendshipsList) {
                SyncFriendshipDTO syncFriendshipDTO = new SyncFriendshipDTO();
                syncFriendshipDTO.setFriendId(N3d.encode(friendships.getFriendId()));
                syncFriendshipDTO.setDisplayName(friendships.getDisplayName());
                syncFriendshipDTO.setStatus(friendships.getStatus());
                syncFriendshipDTO.setTimestamp(friendships.getTimestamp());
                Users u = friendships.getUsers();
                SyncUserDTO syncUserDTO = new SyncUserDTO();
                if (u != null) {
                    syncUserDTO.setId(N3d.encode(u.getId()));
                    syncUserDTO.setNickname(u.getNickname());
                    syncUserDTO.setPortraitUri(u.getPortraitUri());
                    syncUserDTO.setTimestamp(u.getTimestamp());
                }

                syncFriendshipDTO.setUser(syncUserDTO);
                syncFriendshipDTOList.add(syncFriendshipDTO);
            }
        }
        syncInfoDTO.setFriends(syncFriendshipDTOList);

        List<SyncGroupDTO> syncGroupDTOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(groupsList)) {
            for (GroupMembers groupMembers : groupsList) {
                SyncGroupDTO syncGroupDTO = new SyncGroupDTO();
                syncGroupDTO.setGroupId(N3d.encode(groupMembers.getGroupId()));
                syncGroupDTO.setDisplayName(groupMembers.getDisplayName());
                syncGroupDTO.setIsDeleted(GroupMembers.IS_DELETED_YES.equals(groupMembers.getIsDeleted()) ? true : false);
                syncGroupDTO.setRole(groupMembers.getRole());
                Groups g = groupMembers.getGroups();
                SyncGroupInnerDTO groupInnerDTO = new SyncGroupInnerDTO();
                if (g != null) {
                    groupInnerDTO.setId(N3d.encode(g.getId()));
                    groupInnerDTO.setName(g.getName());
                    groupInnerDTO.setPortraitUri(g.getPortraitUri());
                    groupInnerDTO.setTimestamp(g.getTimestamp());
                    syncGroupDTO.setGroup(groupInnerDTO);
                }
                syncGroupDTOList.add(syncGroupDTO);

            }

        }
        syncInfoDTO.setGroups(syncGroupDTOList);


        List<SyncGroupMemberDTO> syncGroupMemberDTOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(groupMembersList)) {
            for (GroupMembers groupMembers : groupMembersList) {
                SyncGroupMemberDTO syncGroupMemberDTO = new SyncGroupMemberDTO();
                syncGroupMemberDTO.setGroupId(N3d.encode(groupMembers.getGroupId()));
                syncGroupMemberDTO.setDisplayName(groupMembers.getDisplayName());
                syncGroupMemberDTO.setIsDeleted(GroupMembers.IS_DELETED_YES.equals(groupMembers.getIsDeleted()) ? true : false);
                syncGroupMemberDTO.setMemberId(N3d.encode(groupMembers.getMemberId()));
                syncGroupMemberDTO.setRole(groupMembers.getRole());
                syncGroupMemberDTO.setTimestamp(groupMembers.getTimestamp());
                Users u = groupMembers.getUsers();
                SyncUserDTO su = new SyncUserDTO();
                if (u != null) {
                    su.setId(N3d.encode(u.getId()));
                    su.setNickname(u.getNickname());
                    su.setTimestamp(u.getTimestamp());
                    su.setPortraitUri(u.getPortraitUri());
                    syncGroupMemberDTO.setUser(su);
                }
                syncGroupMemberDTOList.add(syncGroupMemberDTO);
            }
        }


        syncInfoDTO.setGroup_members(syncGroupMemberDTOList);
        return syncInfoDTO;

    }

    public Page<Users> getUserList(UserListParam param) throws ServiceException {
        int pageNum = param.getPageNum() == null ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        Page<Users> page = PageHelper.startPage(pageNum, pageSize, "id desc");
        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(param.getId())) {
            criteria.andEqualTo("id", N3d.decode(param.getId()));
        }
        if (!StringUtils.isEmpty(param.getNickname())) {
            criteria.andLike("nickname", "%" + param.getNickname() + "%");
        }
        if (!StringUtils.isEmpty(param.getUserId())) {
            criteria.andEqualTo("phone", param.getUserId());
        }
        if (!StringUtils.isEmpty(param.getStartTime())) {
            criteria.andGreaterThan("createdAt", param.getStartTime());
        }
        if (!StringUtils.isEmpty(param.getEndTime())) {
            criteria.andLessThan("createdAt", param.getEndTime());
        }
        usersService.getByExample(example);
        return page;
    }

    public void setBlock(int id, int status, Integer minute) throws ServiceException {
        String encodeId = N3d.encode(id);
        Users users = usersService.getByPrimaryKey(id);
        if (users == null) {
            throw new ServiceException(ErrorCode.USER_NOT_EXIST);
        }

        if (status == 1) {
            try {
                Result result = rongCloudClient.blockUser(encodeId, minute);
                if (!Constants.CODE_OK.equals(result.getCode())) {
                    log.error("Error: block user failed on IM server, error,code: " + result.getCode());
                    throw new ServiceException(ErrorCode.SERVER_ERROR);
                }
            } catch (Exception e) {
                log.error("Error: block user failed on IM server, error: " + e.getMessage(), e);
                throw new ServiceException(ErrorCode.SERVER_ERROR);
            }
        } else {
            try {
                Result result = rongCloudClient.unblockUser(encodeId);
                if (!Constants.CODE_OK.equals(result.getCode())) {
                    log.error("Error: unblock user failed on IM server, error,code: " + result.getCode());
                    throw new ServiceException(ErrorCode.SERVER_ERROR);
                }
            } catch (Exception e) {
                log.error("Error: unblock user failed on IM server, error: " + e.getMessage(), e);
                throw new ServiceException(ErrorCode.SERVER_ERROR);
            }
        }
    }

    public List<BlockUsers> getBlockUserList() throws ServiceException {
        BlockUserResult result = rongCloudClient.getBlockUserList();
        if (!Constants.CODE_OK.equals(result.getCode())) {
            log.error("Error: get block user list failed on IM server, error,code: " + result.getCode());
            throw new ServiceException(ErrorCode.SERVER_ERROR);
        }

        return result.getUsers();
    }
}

