package com.rudraksha.shopsphere.user.mapper;

import com.rudraksha.shopsphere.user.dto.response.PreferencesResponse;
import com.rudraksha.shopsphere.user.entity.Preferences;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PreferencesMapper {

    PreferencesResponse toPreferencesResponse(Preferences preferences);
}
