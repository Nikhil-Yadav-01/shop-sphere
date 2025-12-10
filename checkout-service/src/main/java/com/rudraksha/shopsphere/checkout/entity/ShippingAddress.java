package com.rudraksha.shopsphere.checkout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {
    @Column(name = "shipping_first_name")
    private String firstName;

    @Column(name = "shipping_last_name")
    private String lastName;

    @Column(name = "shipping_address_line1")
    private String addressLine1;

    @Column(name = "shipping_address_line2")
    private String addressLine2;

    @Column(name = "shipping_city")
    private String city;

    @Column(name = "shipping_state")
    private String state;

    @Column(name = "shipping_postal_code")
    private String postalCode;

    @Column(name = "shipping_country")
    private String country;

    @Column(name = "shipping_phone")
    private String phone;
}