package com.mamikos.kostapi.auth.mapper;

import com.mamikos.kostapi.auth.web.dto.UserResponse;
import com.mamikos.kostapi.user.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
