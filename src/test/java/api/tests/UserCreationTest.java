package api.tests;

import api.clients.UserClient;
import api.helpers.UserGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserCreationTest {

    private UserClient userClient;
    private User user;
    private String accessToken;

    @Step("Setup: Prepare test data (register user)")
    @Before
    public void setUp() {
        userClient = new UserClient();
        user = UserGenerator.randomUser();
        accessToken = null;
    }

    @Test
    @DisplayName("Create unique user successfully")
    @Description("Test checks that a unique user can be created with valid data")
    public void createUserSuccessfully() {
        Response response = userClient.register(user);
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail().toLowerCase()))
                .body("user.name", equalTo(user.getName()))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        accessToken = response.path("accessToken");
    }

    @Test
    @DisplayName("Create already registered user")
    @Description("Test checks that creating a user with credentials that already exist returns an error")
    public void createDuplicateUserFails() {
        Response firstResponse = userClient.register(user);
        firstResponse.then().assertThat().statusCode(SC_OK);
        accessToken = firstResponse.path("accessToken");

        Response secondResponse = userClient.register(user);
        secondResponse.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    @DisplayName("Create user without required email field")
    @Description("Test checks that creating a user without the email field returns an error")
    public void createUserWithoutEmailFails() {
        User userWithoutEmail = UserGenerator.userWithoutEmail();

        userWithoutEmail.setPassword(user.getPassword());
        userWithoutEmail.setName(user.getName());

        Response response = userClient.register(userWithoutEmail);
        response.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @DisplayName("Create user without required password field")
    @Description("Test checks that creating a user without the password field returns an error")
    public void createUserWithoutPasswordFails() {
        User userWithoutPassword = UserGenerator.userWithoutPassword();

        userWithoutPassword.setEmail(user.getEmail());
        userWithoutPassword.setName(user.getName());

        Response response = userClient.register(userWithoutPassword);
        response.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @DisplayName("Create user without required name field")
    @Description("Test checks that creating a user without the name field returns an error")
    public void createUserWithoutNameFails() {
        User userWithoutName = UserGenerator.userWithoutName();

        userWithoutName.setEmail(user.getEmail());
        userWithoutName.setPassword(user.getPassword());

        Response response = userClient.register(userWithoutName);
        response.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Step("Teardown: Clean up test data (delete user)")
    @After
    public void tearDown() {
        if (accessToken != null) {

            userClient.deleteUser(accessToken).then().assertThat().statusCode(SC_ACCEPTED);
        }
    }
}