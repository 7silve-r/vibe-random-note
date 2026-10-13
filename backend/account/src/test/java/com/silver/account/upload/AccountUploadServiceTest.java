package com.silver.account.upload;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.account.support.TestData;
import com.silver.common.exception.BusinessException;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileStorage;
import com.silver.storage.service.FileUploadService;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AccountUploadServiceTest {
    FileStorage storage;
    UserService users;
    PlatformTransactionManager manager;
    SimpleTransactionStatus status;
    AccountUploadService service;

    @BeforeEach
    void setup() {
        storage = mock(FileStorage.class);
        users = mock(UserService.class);
        manager = mock(PlatformTransactionManager.class);
        status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        service = new AccountUploadService(new FileUploadService(storage, manager), users);
    }

    @Test
    void goneUser() {
        assertEquals(
                401,
                assertThrows(BusinessException.class, () -> service.avatar(99, null)).getCode());
        verify(users).getById(99);
        verifyNoInteractions(storage, manager);
    }

    @Test
    void avatarById() throws Exception {
        var user = TestData.user();
        user.setUserPic("/uploads/avatars/old.png");
        when(users.getById(1)).thenReturn(user);
        var file = TestData.png();
        when(storage.upload(file, UploadTarget.AVATAR)).thenReturn("new.png");
        when(users.updateById(any(User.class))).thenReturn(true);
        assertEquals("new.png", service.avatar(1, file).url());
        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).updateById(saved.capture());
        assertEquals(1, saved.getValue().getId());
        assertEquals("new.png", saved.getValue().getUserPic());
        verify(users, never()).lambdaQuery();
        var order = inOrder(manager, storage);
        order.verify(manager).commit(status);
        order.verify(storage).deleteQuietly("/uploads/avatars/old.png");
    }
}
