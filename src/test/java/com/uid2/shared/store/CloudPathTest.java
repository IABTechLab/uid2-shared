package com.uid2.shared.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CloudPathTest {
    @Test
    public void testUriAsCloudPath() throws Exception {
        CloudPath path = new CloudPath("https://remote-file-host.com/myname/toplevel");
        Assertions.assertEquals(
                "https://remote-file-host.com/myname/toplevel",
                path.toString());
        Assertions.assertEquals(
                "https://remote-file-host.com/myname/toplevel/secondlevel",
                path.resolve("secondlevel").toString());
        Assertions.assertEquals(
                "https://remote-file-host.com/myname",
                path.getParent().toString());
    }

    @Test
    public void testFilePathAsCloudPath() throws Exception {
        CloudPath path = new CloudPath("/home/myname/toplevel");
        Assertions.assertEquals(
                "/home/myname/toplevel",
                path.toString());
        Assertions.assertEquals(
                "/home/myname/toplevel/secondlevel",
                path.resolve("secondlevel").toString());
        Assertions.assertEquals(
                "/home/myname",
                path.getParent().toString());
    }

    @Test
    public void testS3PathAsCloudPath() throws Exception {
        CloudPath path = new CloudPath("s3://uid-data");
        Assertions.assertEquals(
                "s3://uid-data",
                path.toString());
        Assertions.assertEquals(
                "s3://uid-data/",
                path.resolve("/").toString());
        Assertions.assertEquals(
                "s3://uid-data/domain/sharing",
                path.resolve("domain/sharing").toString());
        Assertions.assertEquals(
                "s3://uid-data/domain",
                path.resolve("domain/sharing").getParent().toString());
        Assertions.assertEquals(
                "data.json",
                new CloudPath("s3://uid-data/data-storage/data.json").getFileName().toString());
    }
}
