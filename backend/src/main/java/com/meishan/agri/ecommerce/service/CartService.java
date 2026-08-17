package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.dto.CartDTO;
import com.meishan.agri.ecommerce.entity.CartItem;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.CartItemMapper;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public List<CartDTO> list(Long userId) {
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getCreateTime))
                .stream().map(item -> {
                    Product p = productMapper.selectById(item.getProductId());
                    CartDTO dto = new CartDTO();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    if (p != null) {
                        dto.setProductName(p.getName());
                        dto.setProductImage(p.getMainImage());
                        dto.setSpecText(p.getSpecText());
                        dto.setPrice(p.getPrice());
                        dto.setStock(p.getStock());
                    }
                    return dto;
                }).toList();
    }

    public CartItem add(Long userId, Long productId, Integer quantity) {
        Product p = productMapper.selectById(productId);
        if (p == null || !"ON_SALE".equals(p.getStatus())) throw new BizException("商品不可购买");
        CartItem exist = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartItemMapper.updateById(exist);
            return exist;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        cartItemMapper.insert(item);
        return item;
    }

    public void updateQuantity(Long userId, Long id, Integer quantity) {
        CartItem item = owned(userId, id);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    public void remove(Long userId, Long id) {
        cartItemMapper.deleteById(owned(userId, id).getId());
    }

    private CartItem owned(Long userId, Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) throw new BizException("购物车项不存在");
        return item;
    }
}
