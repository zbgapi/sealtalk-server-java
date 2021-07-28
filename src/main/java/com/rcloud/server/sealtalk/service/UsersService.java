package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.dao.UsersMapper;
import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: xiuwei.nie
 * @Date: 2020/7/6
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Service
public class UsersService extends AbstractBaseService<Users, Integer> {

    @Resource
    private UsersMapper mapper;

    @Override
    protected Mapper getMapper() {
        return mapper;
    }

    public String getCurrentUserNickNameWithCache(Integer currentUserId) {

        Assert.notNull(currentUserId,"currentUserId is null");

        String nickName = CacheUtil.get(CacheUtil.NICK_NAME_CACHE_PREFIX + currentUserId);
        if (StringUtils.isEmpty(nickName)) {
            Users users = mapper.selectByPrimaryKey(currentUserId);
            if (users != null) {
                nickName = users.getNickname();
                CacheUtil.set(CacheUtil.NICK_NAME_CACHE_PREFIX + currentUserId, nickName);
            }
        }
        return nickName;
    }

    public List<Users> getUsers(List<Integer> ids) {
        Assert.notEmpty(ids, "ids is empty");

        Example example = new Example(Users.class);
        example.createCriteria().andIn("id", ids);
        return this.getByExample(example);
    }

    public Users getUser(String uid) throws ServiceException {
        Assert.notNull(uid, "ids is empty");

        Example example = new Example(Users.class);
        example.createCriteria().andEqualTo("phone", uid);
        Users u = this.getOneByExample(example);
        if (u == null) {
            throw new ServiceException(ErrorCode.USER_NOT_EXIST);
        }
        return u;
    }

    public List<Integer> batchInsert(List<Users> insertUserList) {
        if (insertUserList.size() > 0) {
            for (int i = 0; i < insertUserList.size(); i+=1000) {
                int endIndex = Math.min(i + 1000, insertUserList.size());
                List<Users> users = insertUserList.subList(i, endIndex);
                mapper.insertBatch(users);
            }
        }
        return insertUserList.stream().filter(e -> e.getId() != null).map(Users::getId).collect(Collectors.toList());
    }
}
