package com.meishan.agri.live.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.live.dto.DanmakuDTO;
import com.meishan.agri.live.dto.RoomDTO;
import com.meishan.agri.live.dto.RoomProductDTO;
import com.meishan.agri.live.entity.LiveDanmaku;
import com.meishan.agri.live.entity.LiveRoom;
import com.meishan.agri.live.service.LiveService;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LiveController {
    private final LiveService liveService;
    private final UserMapper userMapper;

    @GetMapping("/api/live/rooms")
    public Result<List<LiveRoom>> rooms() {
        return Result.ok(liveService.listLiveRooms());
    }

    @GetMapping("/api/live/rooms/{id}")
    public Result<RoomDTO> detail(@PathVariable Long id) {
        return Result.ok(liveService.detail(id));
    }

    @GetMapping("/api/live/rooms/{id}/danmaku")
    public Result<List<LiveDanmaku>> danmaku(@PathVariable Long id) {
        return Result.ok(liveService.danmaku(id));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PostMapping("/api/live/rooms")
    public Result<LiveRoom> create(@RequestBody RoomDTO dto) {
        return Result.ok(liveService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/live/rooms/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoomDTO dto) {
        liveService.update(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/live/rooms/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        liveService.changeStatus(StpUtil.getLoginIdAsLong(), id, body.get("status"));
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/live/rooms/{id}/products")
    public Result<Void> products(@PathVariable Long id, @RequestBody List<RoomProductDTO> items) {
        liveService.setProducts(StpUtil.getLoginIdAsLong(), id, items);
        return Result.ok(null);
    }

    @SaCheckLogin
    @PostMapping("/api/live/rooms/{id}/danmaku")
    public Result<LiveDanmaku> send(@PathVariable Long id, @RequestBody DanmakuDTO dto) {
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        return Result.ok(liveService.send(id, user.getId(), user.getNickname(), dto.getContent()));
    }
}
