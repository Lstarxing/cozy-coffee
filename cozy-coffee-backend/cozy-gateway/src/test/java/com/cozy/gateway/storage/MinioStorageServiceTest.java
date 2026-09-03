package com.cozy.gateway.storage;

import com.cozy.gateway.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MinioStorageServiceTest {

    private StorageProperties props(String publicBaseUrl) {
        StorageProperties p = new StorageProperties();
        p.setType("minio");
        p.setEndpoint("http://minio:9000");
        p.setBucket("cozycoffee");
        p.setAccessKeyId("gateway-key");
        p.setAccessKeySecret("gateway-secret");
        p.setPublicBaseUrl(publicBaseUrl);
        return p;
    }

    private MinioStorageService service(String publicBaseUrl) {
        return new MinioStorageService(props(publicBaseUrl), mock(MinioClient.class));
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "cozy.png", "image/png", new byte[]{1, 2, 3, 4});
    }

    @Test
    void upload_returnsPublicUrl_whenMinioSucceeds() throws Exception {
        MinioClient client = mock(MinioClient.class);
        MinioStorageService service = new MinioStorageService(
                props("http://localhost:9000/media/cozycoffee"), client);

        String url = service.upload(png(), "products");

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertEquals("cozycoffee", args.bucket());
        assertEquals("image/png", args.contentType());
        assertTrue(Pattern.matches("images/products/\\d{4}/\\d{2}/[0-9a-f]{32}\\.png", args.object()),
                "object key should keep relative layout, got " + args.object());

        assertTrue(url.startsWith("http://localhost:9000/media/cozycoffee/images/products/"), url);
        assertTrue(url.endsWith(".png"), url);
    }

    @Test
    void upload_throwsStorageException_whenMinioFails() throws Exception {
        MinioClient client = mock(MinioClient.class);
        doThrow(new IOException("connection refused"))
                .when(client)
                .putObject(org.mockito.ArgumentMatchers.any(PutObjectArgs.class));
        MinioStorageService service = new MinioStorageService(
                props("http://localhost:9000/media/cozycoffee"), client);

        assertThrows(StorageException.class, () -> service.upload(png(), "products"));
    }

    @Test
    void upload_throws_whenPublicBaseUrlMissing() {
        assertThrows(StorageException.class, () -> service(null).upload(png(), "products"));
    }

    @Test
    void upload_rejectsNonImageContentType() {
        MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());
        assertThrows(StorageException.class, () -> service("http://x/media").upload(txt, "products"));
    }
}
