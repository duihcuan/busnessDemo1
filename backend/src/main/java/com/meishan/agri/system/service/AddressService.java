package com.meishan.agri.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.system.dto.AddressDTO;
import com.meishan.agri.system.entity.UserAddress;
import com.meishan.agri.system.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final UserAddressMapper addressMapper;

    public List<UserAddress> list(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId).orderByDesc(UserAddress::getIsDefault));
    }

    @Transactional
    public UserAddress add(Long userId, AddressDTO dto) {
        UserAddress address = new UserAddress();
        copy(address, dto);
        address.setUserId(userId);
        if (address.getIsDefault() == 1) clearDefault(userId);
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public void update(Long userId, Long id, AddressDTO dto) {
        UserAddress address = owned(userId, id);
        copy(address, dto);
        if (address.getIsDefault() == 1) clearDefault(userId);
        addressMapper.updateById(address);
    }

    @Transactional
    public void remove(Long userId, Long id) {
        addressMapper.deleteById(owned(userId, id).getId());
    }

    public UserAddress owned(Long userId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        List<UserAddress> list = addressMapper.selectList(
                Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId));
        list.forEach(a -> { a.setIsDefault(0); addressMapper.updateById(a); });
    }

    private void copy(UserAddress target, AddressDTO dto) {
        target.setReceiver(dto.getReceiver());
        target.setPhone(dto.getPhone());
        target.setProvince(dto.getProvince());
        target.setCity(dto.getCity());
        target.setDistrict(dto.getDistrict());
        target.setDetail(dto.getDetail());
        target.setIsDefault(dto.getIsDefault());
    }
}
