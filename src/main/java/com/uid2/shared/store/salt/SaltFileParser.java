package com.uid2.shared.store.salt;

import com.uid2.shared.model.SaltEntry;

public class SaltFileParser {
    private final IdHashingScheme idHashingScheme;

    public SaltFileParser(IdHashingScheme idHashingScheme) {
        this.idHashingScheme = idHashingScheme;
    }

    public SaltEntry[] parseFile(String saltFileContent, Integer size) {
        var entries = new SaltEntry[size];
        int idx = 0;
        for (String line : saltFileContent.split("\n")) {
            final SaltEntry entry = parseLine(line);
            entries[idx] = entry;
            idx++;
        }
        return entries;
    }

    private SaltEntry parseLine(String line) {
        try {
            final String[] fields = line.split(",");
            final long id = Integer.parseInt(fields[0]);
            final String hashedId = this.idHashingScheme.encode(id);
            final long lastUpdated = Long.parseLong(fields[1]);
            final String salt = fields[2];

            Long refreshFrom = null;
            String previousSalt = null;
            SaltEntry.KeyMaterial currentKey = null;
            SaltEntry.KeyMaterial previousKey = null;

            if (fields.length > 3) {
                refreshFrom = Long.parseLong(fields[3]);
            }
            if (fields.length > 4) {
                previousSalt = fields[4];
            }
            if (fields.length > 7) {
                currentKey = new SaltEntry.KeyMaterial(Integer.parseInt(fields[5]), fields[6], fields[7]);
            }
            if (fields.length > 10) {
                previousKey = new SaltEntry.KeyMaterial(Integer.parseInt(fields[8]), fields[9], fields[10]);
            }

            return new SaltEntry(id, hashedId, lastUpdated, salt, refreshFrom, previousSalt, currentKey, previousKey);
        } catch (Exception e) {
            throw new RuntimeException("Trouble parsing Salt Entry " + line, e);
        }
    }

}
