package com.meishan.agri.live.dto;

import com.meishan.agri.live.dto.LiveProductVO;
import com.meishan.agri.live.entity.LiveRoom;
import lombok.Data;

import java.util.List;

@Data
public class RoomDTO {
    private LiveRoom room;
    private List<LiveProductVO> products;
}
