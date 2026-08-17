package com.meishan.agri.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) { return new Result<>(200, "ok", data); }
    public static <T> Result<T> fail(int code, String message) { return new Result<>(code, message, null); }
}
