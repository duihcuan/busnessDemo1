package com.meishan.agri.system.dto;

import lombok.Data;

@Data
public class WechatLoginRequest {
    private String code;   // wx.login 的 code，为空或调用失败时降级
    private String phone;  // 降级模拟登录使用的手机号
}
