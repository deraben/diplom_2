package api.tests;

import api.clients.OrderClient;
import api.clients.UserClient;
import api.helpers.UserGenerator;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import api.models.Order;
import api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

public class GetUserOrdersTest {

    private UserClient userClient;
    private OrderClient orderClient;
    private User user;
    private String accessToken;
    private List<String> createdOrderIngredients;

    @Step("Fetch valid ingredients from API")
    private List<String> fetchValidIngredientsFromApi() {
        List<String> ingredients = new ArrayList<>();
        Response ingredientsResponse = orderClient.getAllIngredients();
        if (ingredientsResponse.getStatusCode() == SC_OK) {
            List<Map<String, Object>> data = ingredientsResponse.jsonPath().getList("data");
            String bunId = null;
            String mainId = null;

            for (Map<String, Object> ingredient : data) {
                if (ingredient.get("type").equals("bun") && bunId == null) {
                    bunId = (String) ingredient.get("_id");
                } else if (!ingredient.get("type").equals("bun") && mainId == null) {
                    mainId = (String) ingredient.get("_id");
                }
                if (bunId != null && mainId != null) {
                    break;
                }
            }

            if (bunId != null && mainId != null) {
                ingredients.add(bunId);
                ingredients.add(mainId);
                ingredients.add(bunId);
            } else {
                System.err.println("Could not find suitable bun and main ingredient from API for order creation.");
            }

        } else {
            System.err.println("Failed to fetch ingredients from API. Status code: " + ingredientsResponse.getStatusCode());
        }
        return ingredients;
    }


    @Before
    public void setUp() {
        userClient = new UserClient();
        orderClient = new OrderClient();
        user = UserGenerator.randomUser();
        accessToken = null;
        createdOrderIngredients = null;

        Response registerResponse = userClient.register(user);
        if (registerResponse.getStatusCode() == SC_OK) {
            accessToken = registerResponse.path("accessToken");

            createdOrderIngredients = fetchValidIngredientsFromApi();
            if (createdOrderIngredients != null && !createdOrderIngredients.isEmpty()) {
                Order order = new Order(createdOrderIngredients);
                orderClient.createOrder(accessToken, order).then().assertThat().statusCode(SC_OK);
            } else {
                System.err.println("Skipping order creation in setup due to failure fetching ingredients.");
                accessToken = null;
            }
        } else {
            System.err.println("Failed to register user in setup: " + registerResponse.getBody().asString());
        }
    }

    @Test
    @DisplayName("Get orders for authorized user")
    @Description("Test checks retrieving orders for a logged-in user")
    public void getOrdersForAuthorizedUserSuccess() {
        org.junit.Assume.assumeTrue("Access token and created order are required for this test",
                accessToken != null && createdOrderIngredients != null && !createdOrderIngredients.isEmpty());

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