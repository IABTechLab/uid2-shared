package com.uid2.shared.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;

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

    @Test
    public void testCloudPathEquality() throws Exception {
        Assertions.assertEquals(
                new CloudPath("https://website.com/a/b/c"),
                new CloudPath("https://website.com/a/b/c"));
        // Note, the following is a corner case as you have to resolve "/next" instead of "next"
        // This should be unnoticeable in practice
        Assertions.assertEquals(
                new CloudPath("https://website.com/next"),
                new CloudPath("https://website.com").resolve("/next"));
        Assertions.assertEquals(
                new CloudPath("https://website.com/next/then"),
                new CloudPath("https://website.com/next").resolve("then"));
        Assertions.assertEquals(
                new CloudPath("/var/tmp/file.txt"),
                new CloudPath("/var/tmp/file.txt"));
        Assertions.assertEquals(
                new CloudPath("/var/tmp"),
                new CloudPath("/var/tmp/file.txt").getParent());
        Assertions.assertNotEquals(
                new CloudPath("/a/b/c"),
                new CloudPath("https://website.com/a/b/c"));
    }

    @Test
    public void testCloudPathCanBeKey() throws Exception {
        HashMap<CloudPath, Integer> pathCount = new HashMap<>();
        pathCount.put(new CloudPath("https://uidapi.com/1"), 1);
        pathCount.put(new CloudPath("http://uidapi.com/2"), 2);
        pathCount.put(new CloudPath("s3://uidapi/3"), 3);
        pathCount.put(new CloudPath("/var/tmp/uidapi/a4"), 4);

        Assertions.assertEquals(1, pathCount.get(new CloudPath("https://uidapi.com/1")));
        Assertions.assertEquals(2, pathCount.get(new CloudPath("http://uidapi.com/2")));
        Assertions.assertEquals(3, pathCount.get(new CloudPath("s3://uidapi/3")));
        Assertions.assertEquals(4, pathCount.get(new CloudPath("/var/tmp/uidapi/a4")));

        pathCount.put(new CloudPath("https://uidapi.com/1"), 5);
        Assertions.assertEquals(5, pathCount.get(new CloudPath("https://uidapi.com/1")));
        Assertions.assertEquals(4, pathCount.size());
    }
}
