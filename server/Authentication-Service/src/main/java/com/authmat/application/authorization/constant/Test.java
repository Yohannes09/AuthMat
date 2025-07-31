package com.authmat.application.authorization.constant;

public class Test {
    public static void main(String[] args) {
        DefaultPermissions.getAll()
                .stream()
                .map(DefaultPermissions::getName)
                .toList()
                .forEach(System.out::println);
    }
}
