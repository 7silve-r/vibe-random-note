package com.silver.account.upload;

import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.common.exception.BusinessException;
import com.silver.storage.model.UploadResult;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountUploadService {
    private final FileUploadService uploads;
    private final UserService userService;

    public AccountUploadService(FileUploadService uploads, UserService userService) {
        this.uploads = uploads;
        this.userService = userService;
    }

    public UploadResult avatar(Integer userId, MultipartFile file) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException(401, "账号不存在，请重新登录");
        if (file == null) throw new BusinessException("请选择头像文件");
        return uploads.replace(
                file,
                UploadTarget.AVATAR,
                user.getUserPic(),
                url -> {
                    User update = new User();
                    update.setId(user.getId());
                    update.setUserPic(url);
                    if (!userService.updateById(update)) throw new BusinessException("头像更新失败");
                });
    }
}
