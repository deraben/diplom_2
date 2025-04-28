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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

public class OrderCreationTest {

    private UserClient userClient;
    private OrderClient orderClient;
    private User user;
    private String accessToken;
    private List<String> validIngredients;

    @Step("Setup: Register user for authorized tests")
    @Before
    public void setUp() {
        userClient = new UserClient();
        orderClient = new OrderClient();
        user = UserGenerator.randomUser();
        accessToken = null;

        Response registerResponse = userClient.register(user);
        if (registerResponse.getStatusCode() == SC_OK) {
            accessToken = registerResponse.path("accessToken");
        } else {
            System.err.println("Failed to register user in setup: " + registerResponse.getBody().asString());
        }

        try {
            validIngredients = fetchValidIngredientsFromApi();
            if (validIngredients.isEmpty()) {
                throw new RuntimeException("Failed to fetch valid ingredients from API. Cannot run tests requiring them.");
            }
        } catch (Exception e) {
            System.err.println("Error fetching ingredients: " + e.getMessage());
            org.junit.Assume.assumeTrue("Failed to fetch valid ingredients, skipping related tests.", false);
        }
    }

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
                System.err.println("Could not find suitable bun and main ingredient from API.");
            }

        } else {
            System.err.println("Failed to fetch ingredients from API. Status code: " + ingredientsResponse.getStatusCode());
        }
        return ingredients;
    }

    @Step("Create an order without authorization with invalid ingredients")
    public Response createOrderWithoutAuthWithInvalidIngredients(List<String> ingredients) {
        Order order = new Order(ingredients);
        return orderClient.createOrderWithoutAuth(order);
    }

    @Step("Create an order with authorization with invalid ingredients")
    public Response createOrderWithAuthWithInvalidIngredients(String accessToken, List<String> ingredients) {
        Order order = new Order(ingredients);
        return orderClient.createOrder(accessToken, order);
    }

    @Test
    @DisplayName("Create order with authorization and valid ingredients - Success")
    @Description("Verify successful creation of an order by an authorized user with valid ingredients")
    public void createOrderWithAuthAndIngredientsSuccess() {
        org.junit.Assume.assumeTrue("Valid ingredients were not fetched.", validIngredients != null && !validIngredients.isEmpty());
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Failed to get user token for authorized test",
                    accessToken != null);
        }
        Order order = new Order(validIngredients);
        Response response = orderClient.createOrder(accessToken, order);
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", notNullValue())
                .body("order", notNullValue());
    }

    @Test
    @DisplayName("Create order without authorization with valid ingredients - Success")
    @Description("Verify successful creation of an order without authorization (anonymous) with valid ingredients")
    public void createOrderWithoutAuthSucceedsWithIngredients() {
        org.junit.Assume.assumeTrue("Valid ingredients were not fetched.", validIngredients != null && !validIngredients.isEmpty());
        Order order = new Order(validIngredients);
        Response response = orderClient.createOrderWithoutAuth(order);
        response.then()
                .log().all()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", notNullValue());
    }

    @Test
    @DisplayName("Create order with authorization without ingredients - Error 400")
    @Description("Verify error when creating an order by an authorized user with an empty list of ingredients")
    public void createOrderWithAuthNoIngredientsFails() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Failed to get user token for authorized test",
                    accessToken != null);
        }
        Order order = new Order(Collections.emptyList());
        Response response = orderClient.createOrder(accessToken, order);
        response.then()
                .assertThat()
                .statusCode(SC_BAD_REQUEST)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Create order with authorization with null ingredients - Error 400")
    @Description("Verify error when creating an order by an authorized user with null in the ingredients field")
    public void createOrderWithAuthNullIngredientsFails() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Failed to get user token for authorized test",
                    accessToken != null);
        }
        Order order = new Order(null);
        Response response = orderClient.createOrder(accessToken, order);
        response.then()
                .assertThat()
                .statusCode(SC_BAD_REQUEST)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Create order without authorization without ingredients - Error 400")
    @Description("Verify error when creating an order without authorization with an empty list of ingredients")
    public void createOrderWithoutAuthNoIngredientsFails() {
        Order order = new Order(Collections.emptyList());
        Response response = orderClient.createOrderWithoutAuth(order);
        response.then()
                .assertThat()
                .statusCode(SC_BAD_REQUEST)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Create order with authorization with invalid ingredient hash - Error 500")
    @Description("Verify error when creating an order by an authorized user with a non-existent ingredient hash")
    public void createOrderWithAuthWithInvalidHashFails() {
        if (accessToken == null) {
            org.junit.Assume.assumeTrue("Failed to get user token for authorized test",
                    accessToken != null);
        }
        List<String> invalidIngredients = Arrays.asList("invalid_hash_12345", "61c0c5a71d1f82001bdaaa6d");
        Order order = new Order(invalidIngredients);
        Response response = orderClient.createOrder(accessToken, order);
        response.then()
                .assertThat()
                .statusCode(SC_INTERNAL_SERVER_ERROR);

    }

    @Test
    @DisplayName("Create order without authorization with invalid ingredient hash - Error 500")
    @Description("Verify error when creating an order without authorization with a non-existent ingredient hash")
    public void createOrderWithoutAuthWithInvalidHashFails() {
        List<String> invalidIngredients = Arrays.asList("invalid_hash_12345");
        Order order = new Order(invalidIngredients);
        Response response = orderClient.createOrderWithoutAuth(order);
        response.then()
                .assertThat()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Step("Teardown: Delete user")
    @After
    public void tearDown() {
        if (accessToken != null) {
            userClient.deleteUser(accessToken);
        }
    }
}