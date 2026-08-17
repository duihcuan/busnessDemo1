package com.meishan.agri.common;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) { return Result.fail(400, e.getMessage()); }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> notLogin(NotLoginException e) { return Result.fail(401, "未登录或登录已过期"); }

    @ExceptionHandler(cn.dev33.satoken.exception.NotRoleException.class)
    public Result<Void> notRole(cn.dev33.satoken.exception.NotRoleException e) { return Result.fail(403, "无权限"); }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("unexpected error", e);
        return Result.fail(500, "系统异常");
    }
}
