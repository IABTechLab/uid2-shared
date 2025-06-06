package com.uid2.shared.store.salt;

import com.uid2.shared.model.SaltEntry;
import com.uid2.shared.model.SaltEntry.KeyMaterial;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SaltFileParserTest {

    private final IdHashingScheme hashingScheme = new IdHashingScheme("id-prefix", "id secret");
    private final SaltFileParser parser = new SaltFileParser(hashingScheme);

    private final String hashed1 = hashingScheme.encode(1);
    private final String hashed2 = hashingScheme.encode(2);
    private final String hashed3 = hashingScheme.encode(3);

    @Test
    void parsesSaltFileWithMinimalFields() {
        var file = """
1,100,salt1
2,200,salt2
""";
        SaltEntry[] actual = parser.parseFile(file, 2);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", null, null, null, null),
                new SaltEntry(2, hashed2, 200, "salt2", null, null, null, null)
        };

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parsesSaltFileWithAllFields() {
        var file = """
1,100,salt1,1000,old_salt1,10,key_1,key_salt_1,100,old_key_1,old_key_1_salt
2,200,salt2,2000,old_salt2,20,key_2,key_salt_2,200,old_key_2,old_key_2_salt
""";
        SaltEntry[] actual = parser.parseFile(file, 2);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", 1000L, "old_salt1",
                        new KeyMaterial(10, "key_1", "key_salt_1"),
                        new KeyMaterial(100, "old_key_1", "old_key_1_salt")
                ),
                new SaltEntry(2, hashed2, 200, "salt2", 2000L, "old_salt2",
                        new KeyMaterial(20, "key_2", "key_salt_2"),
                        new KeyMaterial(200, "old_key_2", "old_key_2_salt")
                )
        };
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parsesSaltFileWithNullValuesForNewFields() {
        var file = """
1,100,salt1,,,,,,,,
2,200,salt2,,,,,,,,
""";
        SaltEntry[] actual = parser.parseFile(file, 2);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", null, null,null, null),
                new SaltEntry(2, hashed2, 200, "salt2", null, null,null, null)
        };
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parsesSaltFileWithNullValuesForKeyFields() {
        var file = """
1,100,salt1,1000,old_salt1,,,,,,
2,200,salt2,2000,old_salt2,,,,,,
""";
        SaltEntry[] actual = parser.parseFile(file, 2);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", 1000L, "old_salt1",null, null),
                new SaltEntry(2, hashed2, 200, "salt2", 2000L, "old_salt2",null, null)
        };
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parsesSaltFileWithoutEncryptionKeyFields() {
        var file = """
1,100,salt1,1000,old_salt1,10
2,200,salt2,2000,old_salt2,20
""";
        SaltEntry[] actual = parser.parseFile(file, 2);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", 1000L, "old_salt1", null, null),
                new SaltEntry(2, hashed2, 200, "salt2", 2000L, "old_salt2",null, null)
        };
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void canMixDifferentFieldsPresenceInSameFile() {
        var file = """
1,100,salt1,1000,old_salt1,10,key_1,key_salt_1,100,old_key_1,old_key_1_salt
2,200,salt2,2000,old_salt2,20
3,300,salt3,3000
""";
        SaltEntry[] actual = parser.parseFile(file, 3);

        SaltEntry[] expected = new SaltEntry[]{
                new SaltEntry(1, hashed1, 100, "salt1", 1000L, "old_salt1",
                        new KeyMaterial(10, "key_1", "key_salt_1"),
                        new KeyMaterial(100, "old_key_1", "old_key_1_salt")
                ),
                new SaltEntry(2, hashed2, 200, "salt2", 2000L, "old_salt2",null, null),
                new SaltEntry(3, hashed3, 300, "salt3", null, null,null, null)
        };

    }
}
