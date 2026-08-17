package com.meishan.agri.live.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.live.dto.RoomDTO;
import com.meishan.agri.live.dto.RoomProductDTO;
import com.meishan.agri.live.entity.LiveDanmaku;
import com.meishan.agri.live.entity.LiveProduct;
import com.meishan.agri.live.entity.LiveRoom;
import com.meishan.agri.live.mapper.LiveDanmakuMapper;
import com.meishan.agri.live.mapper.LiveProductMapper;
import com.meishan.agri.live.mapper.LiveRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveService {
    private final LiveRoomMapper roomMapper;
    private final LiveProductMapper productMapper;
    private final LiveDanmakuMapper danmakuMapper;

    public List<LiveRoom> listLiveRooms() {
        return roomMapper.selectList(Wrappers.<LiveRoom>lambdaQuery()
                .eq(LiveRoom::getStatus, "LIVE").orderByDesc(LiveRoom::getCreateTime));
    }

    public RoomDTO detail(Long roomId) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException("直播间不存在");
        RoomDTO dto = new RoomDTO();
        dto.setRoom(room);
        dto.setProducts(productMapper.selectList(Wrappers.<LiveProduct>lambdaQuery()
                .eq(LiveProduct::getRoomId, roomId).orderByAsc(LiveProduct::getSort)));
        return dto;
    }

    public LiveRoom create(Long sellerId, RoomDTO dto) {
        LiveRoom room = new LiveRoom();
        room.setSellerId(sellerId);
        room.setTitle(dto.getRoom().getTitle());
        room.setCoverUrl(dto.getRoom().getCoverUrl());
        room.setVideoUrl(dto.getRoom().getVideoUrl());
        room.setStatus("OFFLINE");
        roomMapper.insert(room);
        return room;
    }

    public void update(Long sellerId, Long roomId, RoomDTO dto) {
        LiveRoom room = owned(sellerId, roomId);
        if (dto.getRoom().getTitle() != null) room.setTitle(dto.getRoom().getTitle());
        if (dto.getRoom().getCoverUrl() != null) room.setCoverUrl(dto.getRoom().getCoverUrl());
        if (dto.getRoom().getVideoUrl() != null) room.setVideoUrl(dto.getRoom().getVideoUrl());
        roomMapper.updateById(room);
    }

    public void changeStatus(Long sellerId, Long roomId, String status) {
        if (!"LIVE".equals(status) && !"OFFLINE".equals(status)) throw new BizException("非法状态");
        LiveRoom room = owned(sellerId, roomId);
        room.setStatus(status);
        roomMapper.updateById(room);
    }

    @Transactional
    public void setProducts(Long sellerId, Long roomId, List<RoomProductDTO> items) {
        owned(sellerId, roomId);
        productMapper.delete(Wrappers.<LiveProduct>lambdaQuery().eq(LiveProduct::getRoomId, roomId));
        items.forEach(item -> {
            LiveProduct lp = new LiveProduct();
            lp.setRoomId(roomId);
            lp.setProductId(item.getProductId());
            lp.setLivePrice(item.getLivePrice());
            lp.setSort(item.getSort());
            productMapper.insert(lp);
        });
    }

    public LiveDanmaku send(Long roomId, Long userId, String nickname, String content) {
        LiveDanmaku d = new LiveDanmaku();
        d.setRoomId(roomId);
        d.setUserId(userId);
        d.setNickname(nickname);
        d.setContent(content);
        danmakuMapper.insert(d);
        return d;
    }

    public List<LiveDanmaku> danmaku(Long roomId) {
        return danmakuMapper.selectList(Wrappers.<LiveDanmaku>lambdaQuery()
                .eq(LiveDanmaku::getRoomId, roomId).orderByAsc(LiveDanmaku::getCreateTime));
    }

    private LiveRoom owned(Long sellerId, Long roomId) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null || !room.getSellerId().equals(sellerId)) throw new BizException("直播间不存在");
        return room;
    }
}
