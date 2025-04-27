package api.tests;

import api.clients.OrderClient;
import api.clients.UserClient;
import api.helpers.UserGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import api.models.Order;
import api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

public class GetUserOrdersTest {

    private UserClient userClient;
    private OrderClient orderClient;
    private User user;
    private String accessToken;
    private final List<String> validIngredients = Arrays.asList("61c0c5a71d1f82001bdaaa6d", "61c0c5a71d1f82001bdaaa72");

    @Before
    public void setUp() {
        userClient = new UserClient();
        orderClient = new OrderClient();
        user = UserGenerator.randomUser();
        accessToken = null;

        Response registerResponse = userClient.register(user);
        if (registerResponse.getStatusCode() == SC_OK) {
            accessToken = registerResponse.path("accessToken");
            Order order = new Order(validIngredients);
            orderClient.createOrder(accessToken, order).then().assertThat().statusCode(SC_OK);
        } else {
            System.err.println("Failed to register user in setup: " + registerResponse.getBody().asString());
        }
    }

    @Test
    @DisplayName("Get orders for authorized user")
    @Description("Test checks retrieving orders for a logged-in user")
    public void getOrdersForAuthorizedUserSuccess() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Access token is required for this test", accessToken != null);
        }
        Response response = orderClient.getUserOrders(accessToken);
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("orders", notNullValue())
                .body("orders", instanceOf(List.class))
                .body("orders.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Get orders without authorization")
    @Description("Test checks for failure when attempting to get orders without logging in")
    public void getOrdersWithoutAuthorizationFails() {
        Response response = orderClient.getUserOrdersWithoutAuth();
        response.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @After
    public void tearDown() {
        if (accessToken != null) {
            userClient.deleteUser(accessToken);
        }
    }
}