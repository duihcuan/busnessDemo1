package com.meishan.agri.live.dto;

import com.meishan.agri.live.entity.LiveProduct;
import com.meishan.agri.live.entity.LiveRoom;
import lombok.Data;

import java.util.List;

@Data
public class RoomDTO {
    private LiveRoom room;
    private List<LiveProduct> products;
}
