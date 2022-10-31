package com.uid2.shared.optout;

import com.uid2.shared.cloud.CloudUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptOutCloudSyncTest {
    /*
    private static final String s3folder = "s3_folder_test/upload";
    private static final String logDir = "local_test/log_dir";
    private static final String snapshotDir = "local_test/snapshot_dir";

    @Test(expected = AssertionError.class)
    public void createNullInitParameters1_expectFail() {
        // new OptOutCloudSync(null, "a", "b");
    }

    @Test(expected = AssertionError.class)
    public void createNullInitParameters2_expectFail() {
        // new OptOutCloudSync("a", null, "b");
    }

    @Test(expected = AssertionError.class)
    public void createNullInitParameters3_expectFail() {
        // new OptOutCloudSync("a", "b", null);
    }

    @Test(expected = AssertionError.class)
    public void createNullInitParameters4_expectFail() {
        // new OptOutCloudSync(null, null, null);
    }

    @Test
    public void createMapper_thenMapLogFile() {
        OptOutCloudSync mapper = null; // new OptOutCloudSync(s3folder, logDir, snapshotDir);

        String l = TestUtils.newLogFileName();
        String ml = mapper.toLocalPath(l);
        String mr = mapper.toCloudPath(l);
        // System.out.format("%s %s %s\n", l, ml, mr);

        // verify mapped local and remote are different but still a valid log file
        assertNotEquals(ml, mr);
        assertTrue(checkPrefix(mr, s3folder));
        assertTrue(checkPrefix(ml, logDir));
        assertTrue(OptOutUtils.isDeltaFile(ml));
        assertTrue(OptOutUtils.isDeltaFile(mr));

        // verify mapped local and remote can be mapped again to the same local
        assertEquals(ml, mapper.toLocalPath(ml));
        assertEquals(ml, mapper.toLocalPath(mr));

        // verify mapped local and remote can be mapped again to the same remote
        assertEquals(mr, mapper.toCloudPath(ml));
        assertEquals(mr, mapper.toCloudPath(mr));
    }

    @Test
    public void createMapper_thenMapSnapshotFile() {
        OptOutCloudSync mapper = null; // new OptOutCloudSync(s3folder, logDir, snapshotDir);

        String l = TestUtils.newSnapshotFileName();
        String ml = mapper.toLocalPath(l);
        String mr = mapper.toCloudPath(l);
        // System.out.format("%s %s %s\n", l, ml, mr);

        // verify mapped local and remote are different but still a valid log file
        assertNotEquals(ml, mr);
        assertTrue(checkPrefix(mr, s3folder));
        assertTrue(checkPrefix(ml, snapshotDir));
        assertTrue(OptOutUtils.isPartitionFile(ml));
        assertTrue(OptOutUtils.isPartitionFile(mr));

        // verify mapped local and remote can be mapped again to the same local
        assertEquals(ml, mapper.toLocalPath(ml));
        assertEquals(ml, mapper.toLocalPath(mr));

        // verify mapped local and remote can be mapped again to the same remote
        assertEquals(mr, mapper.toCloudPath(ml));
        assertEquals(mr, mapper.toCloudPath(mr));
    }

    @Test
    public void toRemote_checkDateFolderCorrect() {
        OptOutCloudSync mapper = null; // new OptOutCloudSync(s3folder, logDir, snapshotDir);
        checkDateFolderCorrect(mapper, "/opt/uid/optout-log-000_2020-01-01T00.00.00Z_f39955ff.dat", "2020-01-01");
        checkDateFolderCorrect(mapper, "/opt/uid/optout-log-000_2020-01-01T23.59.59Z_f39955ff.dat", "2020-01-01");
        checkDateFolderCorrect(mapper, "/opt/uid/optout-log-000_2020-01-01T12.59.59Z_f39955ff.dat", "2020-01-01");
    }

    private boolean checkPrefix(String path, String prefix) {
        return CloudUtils.normalizPath(path).startsWith(prefix);
    }

    private void checkDateFolderCorrect(OptOutCloudSync mapper, String input, String dateStr) {
        String local = input;
        String remote = mapper.toCloudPath(local);
        assertTrue(remote.startsWith(s3folder));
        assertTrue(remote.contains(dateStr));
        assertEquals(OptOutUtils.getFileTimestamp(local), OptOutUtils.getFileTimestamp(remote));
    }
     */
}
