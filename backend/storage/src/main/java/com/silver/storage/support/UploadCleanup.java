package com.silver.storage.support;

import com.silver.storage.service.FileStorage;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class UploadCleanup {
    private UploadCleanup() {}

    public static void afterCommit(FileStorage storage, String url) {
        if (url == null || url.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            storage.deleteQuietly(url);
                        }
                    });
        } else {
            storage.deleteQuietly(url);
        }
    }

    public static boolean afterRollback(FileStorage storage, String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) storage.deleteQuietly(url);
                    }
                });
        return true;
    }
}
