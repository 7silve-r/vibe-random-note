package com.silver.storage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.storage.model.UploadTarget;
import com.silver.storage.support.UploadCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class FileUploadServiceTest {
    @Test
    void rollback() {
        var storage = mock(FileStorage.class);
        when(storage.upload(null, UploadTarget.MUSIC_USER_AVATAR)).thenReturn("new-url");
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var service = new FileUploadService(storage, manager);
        assertThrows(
                IllegalStateException.class,
                () ->
                        service.upload(
                                null,
                                UploadTarget.MUSIC_USER_AVATAR,
                                url -> {
                                    throw new IllegalStateException("database");
                                }));
        verify(storage).deleteQuietly("new-url");
        verify(manager).rollback(any());
    }

    @Test
    void commitFail() {
        var storage = mock(FileStorage.class);
        when(storage.upload(null, UploadTarget.MUSIC_USER_AVATAR)).thenReturn("new-url");
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new IllegalStateException("commit")).when(manager).commit(any());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new FileUploadService(storage, manager)
                                .upload(null, UploadTarget.MUSIC_USER_AVATAR, url -> {}));
        verify(storage).deleteQuietly("new-url");
    }

    @Test
    void cleanup() {
        var storage = mock(FileStorage.class);
        TransactionSynchronizationManager.initSynchronization();
        try {
            UploadCleanup.afterCommit(storage, "old-url");
            verifyNoInteractions(storage);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            verify(storage).deleteQuietly("old-url");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
