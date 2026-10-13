package com.silver.storage.service;

import com.silver.storage.model.UploadResult;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.support.UploadCleanup;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {
    private final FileStorage storage;
    private final TransactionTemplate transactions;

    public FileUploadService(FileStorage storage, PlatformTransactionManager manager) {
        this.storage = storage;
        this.transactions = new TransactionTemplate(manager);
    }

    public UploadResult upload(MultipartFile file, UploadTarget target, Consumer<String> update) {
        return persist(storage.upload(file, target), null, true, update);
    }

    public UploadResult replace(
            MultipartFile file, UploadTarget target, String oldUrl, Consumer<String> update) {
        return persist(
                file == null ? oldUrl : storage.upload(file, target), oldUrl, file != null, update);
    }

    private UploadResult persist(
            String url, String oldUrl, boolean uploaded, Consumer<String> update) {
        boolean[] registered = {false};
        try {
            transactions.executeWithoutResult(
                    status -> {
                        if (uploaded) registered[0] = UploadCleanup.afterRollback(storage, url);
                        update.accept(url);
                        if (uploaded && registered[0]) UploadCleanup.afterCommit(storage, oldUrl);
                    });
        } catch (RuntimeException ex) {
            if (uploaded && !registered[0]) storage.deleteQuietly(url);
            throw ex;
        }
        if (uploaded && !registered[0]) storage.deleteQuietly(oldUrl);
        return new UploadResult(url);
    }
}
