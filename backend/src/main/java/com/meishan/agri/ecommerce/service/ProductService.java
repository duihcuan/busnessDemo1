package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.dto.ProductDTO;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    public Page<Product> page(Long categoryId, String keyword, String origin, int page, int size, boolean onlyOnSale) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(categoryId != null, Product::getCategoryId, categoryId)
          .like(keyword != null && !keyword.isBlank(), Product::getName, keyword)
          .eq(origin != null && !origin.isBlank(), Product::getOrigin, origin)
          .eq(onlyOnSale, Product::getStatus, "ON_SALE")
          .orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(Page.of(page, size), qw);
    }

    public Product getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BizException("商品不存在");
        return p;
    }

    public Product create(Long sellerId, ProductDTO dto) {
        Product p = new Product();
        apply(p, dto);
        p.setSellerId(sellerId);
        p.setStatus("ON_SALE");
        p.setSoldCount(0);
        productMapper.insert(p);
        return p;
    }

    public void update(Long sellerId, Long id, ProductDTO dto) {
        Product p = owned(sellerId, id);
        apply(p, dto);
        productMapper.updateById(p);
    }

    public void changeStatus(Long sellerId, Long id, String status) {
        if (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)) throw new BizException("非法状态");
        Product p = owned(sellerId, id);
        p.setStatus(status);
        productMapper.updateById(p);
    }

    public void delete(Long sellerId, Long id) {
        productMapper.deleteById(owned(sellerId, id).getId());
    }

    public Product owned(Long sellerId, Long id) {
        Product p = productMapper.selectById(id);
        if (p == null || !p.getSellerId().equals(sellerId)) throw new BizException("商品不存在");
        return p;
    }

    private void apply(Product p, ProductDTO dto) {
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setMainImage(dto.getMainImage());
        p.setImages(dto.getImages());
        p.setSpecText(dto.getSpecText());
        p.setPrice(dto.getPrice());
        p.setStock(dto.getStock());
        p.setOrigin(dto.getOrigin());
        p.setTraceCode(dto.getTraceCode());
        p.setDescription(dto.getDescription());
    }
}
