package com.authmat.authentication.constant;

public final class Endpoints {
    public static final String API_BASE = "/api/v1";

    private Endpoints(){}


    public static final class Auth {
        public static final String BASE = API_BASE + "/auth";

        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String TOKEN_REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";
    }

    public static final class CredentialUpdate {
        public static final String BASE = API_BASE + "/credentials";

        public static final String UPDATE_USERNAME = "/username";
        public static final String UPDATE_EMAIL = "/email";
        public static final String UPDATE_PASSWORD = "/password";
    }

    public static final class Developer{
        public static final String SWAGGER_UI = "/swagger-ui.html";
        public static final String OPEN_API_SPECIFICATION = "/v3/api-docs";
    }

    public static final class SigningKey{
        public static final String BASE = "/signing-key";
    }

    public static final class Role{
        public static final String BASE = "/roles";

        public static final String DomainRole = "/";
        public static final String AddUserRoles = "/user";
    }

}
