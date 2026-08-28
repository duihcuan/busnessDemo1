package com.meishan.agri.system.config;

import cn.dev33.satoken.stp.StpInterface;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
                if (user == null) return List.of();
        if ("ADMIN".equals(user.getRole())) return List.of("ADMIN", "SELLER");
        return List.of(user.getRole());
    }
}
