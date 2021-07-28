package com.rcloud.server.sealtalk.exchange.service;

import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.exchange.dao.EcUserMapper;
import com.rcloud.server.sealtalk.exchange.dao.MerchantMapper;
import com.rcloud.server.sealtalk.exchange.domain.EcUser;
import com.rcloud.server.sealtalk.exchange.domain.Merchant;
import javafx.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class EcUserService {

    @Resource
    private EcUserMapper ecUserMapper;

    @Resource
    private MerchantMapper merchantMapper;

    public EcUser getUser(String userId) throws ServiceException {
        EcUser model = new EcUser();
        model.setUserId(userId);
        EcUser ecUser = ecUserMapper.selectOne(model);
        if (ecUser == null) {
            throw new ServiceException(ErrorCode.USER_NOT_EXIST);
        }

        Merchant model2 = new Merchant();
        model2.setUserId(userId);
        Merchant merchant = merchantMapper.selectOne(model2);
        if (merchant != null) {
            ecUser.setNickName(merchant.getNickName());
            ecUser.setHeadImg(merchant.getHeadUrl());
        }

        return ecUser;
    }

    public List<EcUser> getUsers(List<String> userIdList) throws ServiceException {
        if (userIdList.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }

        Example model = new Example(EcUser.class);
        model.createCriteria().andIn("userId", userIdList);

        List<EcUser> userList = ecUserMapper.selectByExample(model);

        if (userList.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }
        Example model2 = new Example(Merchant.class);
        model2.createCriteria().andIn("userId", userIdList);
        List<Merchant> merchantList = merchantMapper.selectByExample(model2);
        for (Merchant merchant : merchantList) {
            for (EcUser ecUser : userList) {
                if (merchant.getUserId().equals(ecUser.getUserId())) {
                    ecUser.setNickName(merchant.getNickName());
                    ecUser.setHeadImg(merchant.getHeadUrl());
                    break;
                }
            }
        }

        return userList;
    }
}
