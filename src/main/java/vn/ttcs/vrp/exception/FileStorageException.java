package vn.ttcs.vrp.exception;

import java.io.IOException;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, IOException ex) {
        super(message);
    }
}
