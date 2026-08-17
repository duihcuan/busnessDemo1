package com.meishan.agri.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.system.dto.SellerApplyDTO;
import com.meishan.agri.system.entity.SellerInfo;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.SellerInfoMapper;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final SellerInfoMapper sellerInfoMapper;
    private final UserMapper userMapper;

    @Transactional
    public SellerInfo apply(Long userId, SellerApplyDTO dto) {
        SellerInfo info = sellerInfoMapper.selectOne(
                Wrappers.<SellerInfo>lambdaQuery().eq(SellerInfo::getUserId, userId));
        if (info == null) {
            info = new SellerInfo();
            info.setUserId(userId);
        }
        info.setShopName(dto.getShopName());
        info.setShopDesc(dto.getShopDesc());
        info.setStatus("PENDING");
        if (info.getId() == null) sellerInfoMapper.insert(info);
        else sellerInfoMapper.updateById(info);
        return info;
    }

    public SellerInfo myInfo(Long userId) {
        return sellerInfoMapper.selectOne(
                Wrappers.<SellerInfo>lambdaQuery().eq(SellerInfo::getUserId, userId));
    }

    public List<SellerInfo> listByStatus(String status) {
        return sellerInfoMapper.selectList(
                Wrappers.<SellerInfo>lambdaQuery()
                        .eq(status != null && !status.isBlank(), SellerInfo::getStatus, status));
    }

    @Transactional
    public void approve(Long sellerInfoId) {
        setStatus(sellerInfoId, "APPROVED");
    }

    @Transactional
    public void reject(Long sellerInfoId) {
        setStatus(sellerInfoId, "REJECTED");
    }

    private void setStatus(Long sellerInfoId, String status) {
        SellerInfo info = sellerInfoMapper.selectById(sellerInfoId);
        if (info == null) throw new BizException("商家信息不存在");
        info.setStatus(status);
        sellerInfoMapper.updateById(info);
        if ("APPROVED".equals(status)) {
            User user = userMapper.selectById(info.getUserId());
            user.setRole("SELLER");
            userMapper.updateById(user);
        }
    }
}
