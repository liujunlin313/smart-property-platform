package com.smartproperty.platform.file;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;

/**
 * Test-only cleanup utility for end-to-end storage probes.
 * Credentials are read from the process environment and are never command-line arguments.
 */
public final class MinioObjectCleanupTool {
    private MinioObjectCleanupTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected bucket and object key");
        }
        MinioClient client = MinioClient.builder()
                .endpoint(required("MINIO_ENDPOINT"))
                .credentials(required("MINIO_ACCESS_KEY"), required("MINIO_SECRET_KEY"))
                .build();
        client.removeObject(RemoveObjectArgs.builder().bucket(args[0]).object(args[1]).build());
        System.out.println("MINIO_TEST_OBJECT_REMOVED");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
