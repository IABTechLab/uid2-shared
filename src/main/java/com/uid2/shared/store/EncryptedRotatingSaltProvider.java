package com.uid2.shared.store;

import com.uid2.shared.Const;
import com.uid2.shared.cloud.DownloadCloudStorage;
import com.uid2.shared.model.SaltEntry;
import com.uid2.shared.store.reader.RotatingCloudEncryptionKeyProvider;
import com.uid2.shared.store.scope.StoreScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.uid2.shared.util.CloudEncryptionHelpers.decryptInputStream;

public class EncryptedRotatingSaltProvider extends RotatingSaltProvider {
    private final RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider;

    public EncryptedRotatingSaltProvider(DownloadCloudStorage fileStreamProvider, RotatingCloudEncryptionKeyProvider cloudEncryptionKeyProvider, StoreScope scope) {
        super(fileStreamProvider, scope.getMetadataPath().toString());
        this.cloudEncryptionKeyProvider = cloudEncryptionKeyProvider;
    }

    @Override
    protected SaltEntry[] readInputStream(InputStream inputStream, SaltEntryBuilder entryBuilder, Integer size) throws IOException {
        String decrypted = decryptInputStream(inputStream, cloudEncryptionKeyProvider);
        SaltEntry[] entries = new SaltEntry[size];
        int idx = 0;
        for (String line : decrypted.split("\n")) {
            final SaltEntry entry = entryBuilder.toEntry(line);
            entries[idx] = entry;
            idx++;
        }
        return entries;
    }
}
