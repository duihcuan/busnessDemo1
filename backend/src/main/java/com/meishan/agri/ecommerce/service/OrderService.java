package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.OrderStatus;
import com.meishan.agri.ecommerce.dto.OrderCreateDTO;
import com.meishan.agri.ecommerce.dto.OrderDTO;
import com.meishan.agri.ecommerce.dto.OrderItemDTO;
import com.meishan.agri.ecommerce.dto.ShipDTO;
import com.meishan.agri.ecommerce.entity.OrderItem;
import com.meishan.agri.ecommerce.entity.Orders;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.OrderItemMapper;
import com.meishan.agri.ecommerce.mapper.OrderMapper;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import com.meishan.agri.system.entity.UserAddress;
import com.meishan.agri.system.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper addressMapper;

    @Transactional
    public Orders create(Long userId, OrderCreateDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new BizException("订单不能为空");
        UserAddress address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) throw new BizException("收货地址不存在");

        BigDecimal total = BigDecimal.ZERO;
        Long sellerId = null;
        for (OrderItemDTO item : dto.getItems()) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || !"ON_SALE".equals(p.getStatus())) throw new BizException("商品不可购买");
            if (p.getStock() < item.getQuantity()) throw new BizException("商品库存不足：" + p.getName());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            sellerId = p.getSellerId();
        }

        Orders order = new Orders();
        order.setOrderNo("M" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000));
        order.setUserId(userId);
        order.setSellerId(sellerId);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING_PAY.name());
        order.setReceiverName(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        for (OrderItemDTO item : dto.getItems()) {
            Product p = productMapper.selectById(item.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductImage(p.getMainImage());
            oi.setSpecText(p.getSpecText());
            oi.setPrice(p.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setSubtotal(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
            p.setStock(p.getStock() - item.getQuantity());
            productMapper.updateById(p);
        }
        return order;
    }

    @Transactional
    public Orders pay(Long userId, Long orderId, String channel) {
        Orders o = ownedByUser(userId, orderId);
        o.setStatus(OrderStatus.valueOf(o.getStatus()).to(OrderStatus.PAID).name());
        o.setPayTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders cancel(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.CANCELLED);
        restoreStock(o);
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders ship(Long sellerId, Long orderId, ShipDTO dto) {
        Orders o = ownedBySeller(sellerId, orderId);
        o.setStatus(OrderStatus.valueOf(o.getStatus()).to(OrderStatus.SHIPPED).name());
        o.setLogisticsCompany(dto.getLogisticsCompany());
        o.setLogisticsNo(dto.getLogisticsNo());
        o.setShipTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders confirm(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.COMPLETED);
        o.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders refund(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.REFUNDED);
        restoreStock(o);
        orderMapper.updateById(o);
        return o;
    }

    public Page<Orders> pageByUser(Long userId, int page, int size) {
        return orderMapper.selectPage(Page.of(page, size),
                Wrappers.<Orders>lambdaQuery().eq(Orders::getUserId, userId).orderByDesc(Orders::getCreateTime));
    }

    public Page<Orders> pageBySeller(Long sellerId, int page, int size) {
        return orderMapper.selectPage(Page.of(page, size),
                Wrappers.<Orders>lambdaQuery().eq(Orders::getSellerId, sellerId).orderByDesc(Orders::getCreateTime));
    }

    public OrderDTO detail(Long userId, Long orderId, boolean seller) {
        Orders o = seller ? ownedBySeller(userId, orderId) : ownedByUser(userId, orderId);
        OrderDTO dto = new OrderDTO();
        dto.setOrder(o);
        dto.setItems(orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId)));
        return dto;
    }

    private Orders ownedByUser(Long userId, Long orderId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) throw new BizException("订单不存在");
        return o;
    }

    private Orders ownedBySeller(Long sellerId, Long orderId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null || !o.getSellerId().equals(sellerId)) throw new BizException("订单不存在");
        return o;
    }

    private void transition(Orders o, OrderStatus target) {
        o.setStatus(OrderStatus.valueOf(o.getStatus()).to(target).name());
    }

    private void restoreStock(Orders o) {
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, o.getId()));
        for (OrderItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productMapper.updateById(p);
            }
        }
    }
}
