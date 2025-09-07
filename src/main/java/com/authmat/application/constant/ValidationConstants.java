package com.authmat.application.constant;

public class ValidationConstants {

    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9]{5,15}$";
    public static final String USERNAME_VALIDATION_MESSAGE = "Username should be 5-15 characters long with no special symbols.";

    public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9!@#$%^&*]).{8,}$";
    public static final String PASSWORD_VALIDATION_MESSAGE = "Password must be at least 8 characters long, have at least 1 capital letter, and a number or special character.";

    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    public static final String EMAIL_VALIDATION_MESSAGE = "Enter a valid email, e.g., 'user@example.com'.";

    public static final String USERNAME_OR_EMAIL_PATTERN =
            "^(?=.{3,50}$)([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|[a-zA-Z0-9_]{3,50})$";
    public static final String USERNAME_OR_EMAIL_VALIDATION_MESSAGE = "Your username or email is invalid";


}


