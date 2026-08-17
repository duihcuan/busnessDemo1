package com.meishan.agri.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.system.dto.AddressDTO;
import com.meishan.agri.system.entity.UserAddress;
import com.meishan.agri.system.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @SaCheckLogin
    @GetMapping
    public Result<List<UserAddress>> list() {
        return Result.ok(addressService.list(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckLogin
    @PostMapping
    public Result<UserAddress> add(@RequestBody AddressDTO dto) {
        return Result.ok(addressService.add(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AddressDTO dto) {
        addressService.update(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        addressService.remove(StpUtil.getLoginIdAsLong(), id);
        return Result.ok(null);
    }
}
