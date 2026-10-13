package com.silver.account.support;

import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.account.utils.SecurityUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TestData {
    private TestData() {}

    public static void authenticate(Integer id) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new SecurityUtil.Account(id, "writer" + id),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));
        SecurityContextHolder.setContext(context);
    }

    @SuppressWarnings("unchecked")
    public static LambdaQueryChainWrapper<User> users(UserService service) {
        LambdaQueryChainWrapper<User> query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(query).eq(any(), any());
        when(service.lambdaQuery()).thenReturn(query);
        return query;
    }

    public static User user() {
        User user = new User();
        user.setId(1);
        user.setUsername("writer01");
        return user;
    }

    public static MockMultipartFile png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "../../fake.html", "text/html", output.toByteArray());
    }
}
