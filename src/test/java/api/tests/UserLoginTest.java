package api.tests;

import api.clients.UserClient;
import api.helpers.UserGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import api.models.Credentials;
import api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserLoginTest {

    private UserClient userClient;
    private User user;
    private Credentials credentials;
    private String accessToken;

    @Step("Setup: Prepare test data (register user)")
    @Before
    public void setUp() {
        userClient = new UserClient();
        user = UserGenerator.randomUser();

        Response registerResponse = userClient.register(user);
        registerResponse.then().assertThat().statusCode(SC_OK);
        accessToken = registerResponse.path("accessToken");
        credentials = Credentials.fromUser(user);
    }

    @Test
    @DisplayName("Login with existing user credentials")
    @Description("Test checks successful login with valid email and password")
    public void loginWithExistingUserSuccess() {
        Response loginResponse = userClient.login(credentials);
        loginResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail().toLowerCase()))
                .body("user.name", equalTo(user.getName()))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("Login with incorrect email")
    @Description("Test checks login failure with incorrect email and correct password")
    public void loginWithIncorrectEmailFails() {
        Credentials wrongCredentials = new Credentials("wrong+" + credentials.getEmail(), credentials.getPassword());
        Response loginResponse = userClient.login(wrongCredentials);
        loginResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Test
    @DisplayName("Login with incorrect password")
    @Description("Test checks login failure with correct email and incorrect password")
    public void loginWithIncorrectPasswordFails() {
        Credentials wrongCredentials = new Credentials(credentials.getEmail(), credentials.getPassword() + "wrong");
        Response loginResponse = userClient.login(wrongCredentials);
        loginResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Step("Teardown: Clean up test data (delete user)")
    @After
    public void tearDown() {
        if (accessToken != null) {
            userClient.deleteUser(accessToken).then().assertThat().statusCode(SC_ACCEPTED);
        }
    }
}