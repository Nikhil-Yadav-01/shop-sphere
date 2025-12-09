package com.rudraksha.shopsphere.shared.security;

import javax.crypto.SecretKey;

public interface KeyRotationService {

    SecretKey getCurrentKey();

    SecretKey getKeyById(String keyId);

    void rotateKey();

    String getCurrentKeyId();
}
