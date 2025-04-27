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
import org.apache.commons.lang3.RandomStringUtils;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserDataUpdateTest {

    private UserClient userClient;
    private User user;
    private String accessToken;

    @Step("Setup: Register user and get token")
    @Before
    public void setUp() {
        userClient = new UserClient();
        user = UserGenerator.randomUser();
        Response registerResponse = userClient.register(user);
        registerResponse.then().assertThat().statusCode(SC_OK);
        accessToken = registerResponse.path("accessToken");
        org.junit.Assume.assumeTrue("Failed to get user token after registration",
                accessToken != null && !accessToken.isEmpty());
    }

    @Test
    @DisplayName("Update user email with authorization - Success")
    @Description("Verify successful update of user email when authorized")
    public void updateUserEmailWithAuthSuccess() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Access token is required for this test", accessToken != null);
        }
        String newEmail = "new_" + user.getEmail();
        User updatedUserData = new User(newEmail, null, null);

        Response updateResponse = userClient.updateUser(accessToken, updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail.toLowerCase()));
        user.setEmail(newEmail);
    }

    @Test
    @DisplayName("Attempt to update user name with authorization - Error 403")
    @Description("Verify that attempting to update user name with authorization returns a 403 error")
    public void updateUserNameWithAuthFailsWith403() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Access token is required for this test", accessToken != null);
        }
        String newName = "NewName" + RandomStringUtils.randomAlphanumeric(5);
        User updatedUserData = new User(null, null, newName);

        Response updateResponse = userClient.updateUser(accessToken, updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Attempt to update user password with authorization - Error 403")
    @Description("Verify that attempting to update user password with authorization returns a 403 error")
    public void updateUserPasswordWithAuthFailsWith403() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Access token is required for this test", accessToken != null);
        }
        String newPassword = RandomStringUtils.randomAlphanumeric(12);
        User updatedUserData = new User(null, newPassword, null);

        Response updateResponse = userClient.updateUser(accessToken, updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Update user email without authorization - Error 401")
    @Description("Verify that user email cannot be updated without authorization")
    public void updateUserEmailWithoutAuthFails() {
        String newEmail = "noauth_" + user.getEmail();
        User updatedUserData = new User(newEmail, null, null);

        Response updateResponse = userClient.updateUserWithoutAuth(updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Update user name without authorization - Error 401")
    @Description("Verify that user name cannot be updated without authorization")
    public void updateUserNameWithoutAuthFails() {
        String newName = "NoAuth" + RandomStringUtils.randomAlphanumeric(5);
        User updatedUserData = new User(null, null, newName);

        Response updateResponse = userClient.updateUserWithoutAuth(updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Update user password without authorization - Error 401")
    @Description("Verify that user password cannot be updated without authorization")
    public void updateUserPasswordWithoutAuthFails() {
        String newPassword = RandomStringUtils.randomAlphanumeric(12);
        User updatedUserData = new User(null, newPassword, null);

        Response updateResponse = userClient.updateUserWithoutAuth(updatedUserData);
        updateResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Teardown: Delete user")
    @After
    public void tearDown() {
        if (accessToken != null) {

            userClient.deleteUser(accessToken);

        }
    }
}