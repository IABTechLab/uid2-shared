package com.uid2.shared.store.salt;

import com.uid2.shared.model.SaltEntry;

public class SaltFileParser {
    private final IdHashingScheme idHashingScheme;

    public SaltFileParser(IdHashingScheme idHashingScheme) {
        this.idHashingScheme = idHashingScheme;
    }

    public SaltEntry[] parseFile(String saltFileContent, Integer size) {
        var lines = saltFileContent.split("\n");
        return parseFileLines(lines, size);
    }

    public SaltEntry[] parseFileLines(String[] saltFileLines, Integer size) {
        var entries = new SaltEntry[size];
        int lineNumber = 0;
        for (String line : saltFileLines) {
            final SaltEntry entry = parseLine(line, lineNumber);
            entries[lineNumber] = entry;
            lineNumber++;
        }
        return entries;
    }

    private SaltEntry parseLine(String line, int lineNumber) {
        try {
            final String[] fields = line.split(",");

            final long id = Integer.parseInt(fields[0]);
            final String hashedId = this.idHashingScheme.encode(id);
            final long lastUpdated = Long.parseLong(fields[1]);
            final String salt = fields[2].isEmpty() ? null : fields[2];
            final Long refreshFrom =  Long.parseLong(fields[3]);

            String previousSalt = null;
            SaltEntry.KeyMaterial currentKeySalt = null;
            SaltEntry.KeyMaterial previousKeySalt = null;

            if (fields.length > 4) {
                previousSalt = fields[4].isEmpty() ? null : fields[4];
            }

            if (fields.length > 7) {
                currentKeySalt = fields[5].isEmpty() ? null : new SaltEntry.KeyMaterial(Integer.parseInt(fields[5]), fields[6], fields[7]);
            }

            if (fields.length > 10) {
                previousKeySalt = fields[8].isEmpty() ? null : new SaltEntry.KeyMaterial(Integer.parseInt(fields[8]), fields[9], fields[10]);
            }

            return new SaltEntry(id, hashedId, lastUpdated, salt, refreshFrom, previousSalt, currentKeySalt, previousKeySalt);
        } catch (Exception e) {
            throw new RuntimeException("Trouble parsing Salt Entry, line number: " + lineNumber, e);
        }
    }

}
