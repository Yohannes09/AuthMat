package com.authmat.application.users.mapper;

import com.authmat.application.users.User;
import com.authmat.application.users.UserDto;

public class Test {
    public static void main(String[] args) {
        UserDto userDto = UserMapper.userMapper.entityToDto(User.builder().username("yo").email("yo@gmail.com").build());
        System.out.println(userDto.getEmail());
    }
}
