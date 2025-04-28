package api.clients;

import api.config.RestAssuredConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import api.models.Order;

import static io.restassured.RestAssured.given;

public class OrderClient {

    private static final String ORDERS_PATH = "/orders";
    private static final String INGREDIENTS_PATH = "/ingredients";

    @Step("Create an order with ingredients: {order.ingredients} using token")
    public Response createOrder(String accessToken, Order order) {
        return given()
                .spec(RestAssuredConfig.getAuthorizedRequestSpec(accessToken))
                .body(order)
                .when()
                .post(ORDERS_PATH);
    }

    @Step("Create an order without authorization using ingredients: {order.ingredients}")
    public Response createOrderWithoutAuth(Order order) {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .body(order)
                .when()
                .post(ORDERS_PATH);
    }

    @Step("Get orders for the user using token")
    public Response getUserOrders(String accessToken) {
        return given()
                .spec(RestAssuredConfig.getAuthorizedRequestSpec(accessToken))
                .when()
                .get(ORDERS_PATH);
    }

    @Step("Get orders without authorization")
    public Response getUserOrdersWithoutAuth() {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .when()
                .get(ORDERS_PATH);
    }

    @Step("Get all ingredients")
    public Response getAllIngredients() {
        return given()
                .spec(RestAssuredConfig.getBaseRequestSpec())
                .when()
                .get(INGREDIENTS_PATH);
    }
}