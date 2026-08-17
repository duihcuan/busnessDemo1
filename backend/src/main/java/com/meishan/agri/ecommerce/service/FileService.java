package com.meishan.agri.ecommerce.service;

import com.meishan.agri.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov");
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");
        if (file.getSize() > MAX_SIZE) throw new BizException("文件不能超过 20MB");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) throw new BizException("不支持的文件类型");

        String dir = "uploads/" + LocalDate.now().toString().replace("-", "");
        try {
            Path path = Paths.get(dir);
            Files.createDirectories(path);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(path.resolve(filename).toFile());
            return "/" + dir + "/" + filename;
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }
    }
}
