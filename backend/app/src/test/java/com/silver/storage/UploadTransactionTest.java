package com.silver.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileStorage;
import com.silver.storage.service.FileUploadService;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

class UploadTransactionTest {
    FileStorage storage = mock(FileStorage.class);
    MockMultipartFile file = new MockMultipartFile("file", new byte[] {1});
    FileUploadService uploads;
    TransactionTemplate transaction;
    JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE upload_record (url VARCHAR(255))");
        jdbc.update("INSERT INTO upload_record VALUES ('old-url')");
        var manager = new DataSourceTransactionManager(source);
        transaction = new TransactionTemplate(manager);
        uploads = new FileUploadService(storage, manager);
        when(storage.upload(file, UploadTarget.ARTICLE_COVER)).thenReturn("new-url");
    }

    void replace() {
        uploads.replace(
                file,
                UploadTarget.ARTICLE_COVER,
                "old-url",
                url -> jdbc.update("UPDATE upload_record SET url = ?", url));
    }

    @Test
    void outerCommit() {
        transaction.executeWithoutResult(
                status -> {
                    replace();
                    verify(storage, never()).deleteQuietly(any());
                });
        assertEquals("new-url", jdbc.queryForObject("SELECT url FROM upload_record", String.class));
        verify(storage).deleteQuietly("old-url");
        verify(storage, never()).deleteQuietly("new-url");
    }

    @Test
    void outerRollback() {
        transaction.executeWithoutResult(
                status -> {
                    replace();
                    status.setRollbackOnly();
                    verify(storage, never()).deleteQuietly(any());
                });
        assertEquals("old-url", jdbc.queryForObject("SELECT url FROM upload_record", String.class));
        verify(storage).deleteQuietly("new-url");
        verify(storage, never()).deleteQuietly("old-url");
    }

    @Test
    void databaseFailure() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        uploads.replace(
                                file,
                                UploadTarget.ARTICLE_COVER,
                                "old-url",
                                url -> {
                                    throw new IllegalStateException("failed update");
                                }));
        verify(storage).deleteQuietly("new-url");
        verify(storage, never()).deleteQuietly("old-url");
    }
}
