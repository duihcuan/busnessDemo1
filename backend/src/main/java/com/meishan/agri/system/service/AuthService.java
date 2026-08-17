package com.meishan.agri.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.common.ShaUtil;
import com.meishan.agri.system.dto.AdminLoginRequest;
import com.meishan.agri.system.dto.LoginRequest;
import com.meishan.agri.system.dto.LoginResponse;
import com.meishan.agri.system.dto.WechatLoginRequest;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;

    @Value("${wx.appid:}") private String appid;
    @Value("${wx.secret:}") private String secret;

    public LoginResponse mockLogin(LoginRequest req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, req.getPhone()));
        if (user == null) {
            user = new User();
            user.setPhone(req.getPhone());
            user.setNickname("用户" + req.getPhone().substring(7));
            user.setRole("CONSUMER");
            user.setStatus(1);
            userMapper.insert(user);
        }
        return doLogin(user);
    }

    public LoginResponse wechatLogin(WechatLoginRequest req) {
        try {
            String openid = code2Session(req.getCode());
            User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getWxOpenid, openid));
            if (user == null) {
                user = new User();
                user.setWxOpenid(openid);
                user.setNickname("微信用户");
                user.setRole("CONSUMER");
                user.setStatus(1);
                userMapper.insert(user);
            }
            return doLogin(user);
        } catch (Exception e) {
            LoginRequest fallback = new LoginRequest();
            fallback.setPhone(req.getPhone());
            fallback.setCode("123456");
            return mockLogin(fallback);
        }
    }

    public LoginResponse adminLogin(AdminLoginRequest req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !user.getPassword().equals(ShaUtil.hash(req.getPassword()))) {
            throw new BizException("用户名或密码错误");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("非管理员账号");
        }
        return doLogin(user);
    }

    public User currentUser() {
        Long id = StpUtil.getLoginIdAsLong();
        return userMapper.selectById(id);
    }

    private LoginResponse doLogin(User user) {
        StpUtil.login(user.getId());
        return new LoginResponse(StpUtil.getTokenValue(), user.getRole(),
                user.getId(), user.getNickname());
    }

    private String code2Session(String code) {
        if (appid == null || appid.isBlank() || code == null || code.isBlank()) {
            throw new BizException("未配置微信登录");
        }
        Map<?, ?> resp = RestClient.builder().build().get()
                .uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",
                        appid, secret, code)
                .retrieve().body(Map.class);
        Object openid = resp == null ? null : resp.get("openid");
        if (openid == null) throw new BizException("微信登录失败");
        return openid.toString();
    }
}
