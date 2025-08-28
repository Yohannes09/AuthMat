package com.authmat.application.authorization.constant;

public class Test {
    public static void main(String[] args) {
        DefaultPermission.getAll()
                .stream()
                .map(DefaultPermission::getName)
                .toList()
                .forEach(System.out::println);
    }
}
