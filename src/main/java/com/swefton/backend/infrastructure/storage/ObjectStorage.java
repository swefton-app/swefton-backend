package com.swefton.backend.infrastructure.storage;

import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface ObjectStorage {

    long store(String key, InputStream content);

    Resource load(String key);

    void delete(String key);
}
