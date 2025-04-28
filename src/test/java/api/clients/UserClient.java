package api.clients;

import api.config.RestAssuredConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import api.models.Credentials;
import api.models.User;

import static io.restassured.RestAssured.given;

public class UserClient {

    private static final String REGISTER_PATH = "/auth/register";
    private static final String LOGIN_PATH = "/auth/login";
    private static final String USER_DATA_PATH = "/auth/user";

    @Step("Register user with credentials: {user.email}, {user.password}, {user.name}")
    public Response register(User user) {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .body(user)
                .when()
                .post(REGISTER_PATH);
    }

    @Step("Login with credentials: {credentials.email}, {credentials.password}")
    public Response login(Credentials credentials) {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .body(credentials)
                .when()
                .post(LOGIN_PATH);
    }

    @Step("Update user data with token. New data: Email={user.email}, Name={user.name}")
    public Response updateUser(String accessToken, User user) {
        return given()
                .spec(RestAssuredConfig.getAuthorizedRequestSpec(accessToken))
                .body(user)
                .when()
                .patch(USER_DATA_PATH);
    }

    @Step("Update user data without authorization. New data: Email={user.email}, Name={user.name}")
    public Response updateUserWithoutAuth(User user) {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .body(user)
                .when()
                .patch(USER_DATA_PATH);
    }


    @Step("Delete user using token")
    public Response deleteUser(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            System.out.println("Skipping delete request as access token is null or empty.");
            return null;
        }
        return given()
                .spec(RestAssuredConfig.getAuthorizedRequestSpec(accessToken))
                .when()
                .delete(USER_DATA_PATH);
    }
}