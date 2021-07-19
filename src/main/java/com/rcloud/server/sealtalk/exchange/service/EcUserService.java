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

import javax.annotation.Resource;

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
}
