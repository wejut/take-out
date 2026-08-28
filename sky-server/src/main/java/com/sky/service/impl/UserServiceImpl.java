package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    WeChatProperties weChatProperties;
    @Autowired
    UserMapper userMapper;

    //微信接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    /**
     *微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxlogin(UserLoginDTO userLoginDTO) {
        //调用微信接口取openid然后判断id error不error还有用户是否新用户
        Map<String, String> fengzhuang = new HashMap<>();
        fengzhuang.put("appid",weChatProperties.getAppid());
        fengzhuang.put("secret",weChatProperties.getSecret());
        fengzhuang.put("js_code",userLoginDTO.getCode());
        fengzhuang.put("grant_type","authorization_code");
        //提取openid
        String vxfanghuizhi = HttpClientUtil.doGet(WX_LOGIN, fengzhuang);
        JSONObject jsonObject = JSON.parseObject(vxfanghuizhi);
        String openid = jsonObject.getString("openid");
        log.info("openid取到了是{}",openid);
        //从数据库用openid查找返回用户
        if (openid == null){
            throw  new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //新用户
        User user = userMapper.getByOpenId(openid);
        if(user == null){
             user = User.builder()
                     .openid(openid)
                     .createTime(LocalDateTime.now())
                     .build();
             userMapper.insert(user);
        }
        return user;
    }
}
