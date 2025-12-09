package com.rudraksha.shopsphere.shared.security;

public interface TokenRevocationStore {

    void revokeToken(String tokenId);

    boolean isRevoked(String tokenId);
}
