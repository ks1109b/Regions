package ru.twogis.api;

import io.restassured.builder.*;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RegionsTest {

    private final Set<String> validCountryCode = Set.of("ru", "kg", "kz", "cz");
    private final String ID_PATH = "items.id";
    private final String ITEMS_PATH = "items";
    private final String COUNTRY_CODE_PATH = "items.country.code";
    private final String PAGE1_SIZE15 = "/regions?page=1&page_size=15";
    private final String PAGE2_SIZE15 = "/regions?page=2&page_size=15";
    private final String PAGE1 = "/regions?page=1";
    private final String PAGE_DEFAULT = "/regions";
    private final int PAGE_SIZE = 15;

    private final RequestSpecification requestSpec = new RequestSpecBuilder()
            .setBaseUri("https://regions-test.2gis.com")
            .setBasePath("/1.0")
            .setContentType(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();

    private final ResponseSpecification responseSpec = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody(matchesJsonSchemaInClasspath("regions.schema.json"))
            .build();

    @Test
    void shouldGetTotal() {
        List<Integer> first = given().spec(requestSpec)
                .when().get(PAGE1_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);
        List<Integer> second = given().spec(requestSpec)
                .when().get(PAGE2_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);
        var allId = new ArrayList<>(first);
        allId.addAll(second);
        List<Integer> uniqueElements = allId.stream().distinct().collect(Collectors.toList());

        assertThat(allId.size(), is(uniqueElements.size()));
    }

    @Test
    void shouldAreAllItemsUnique() {
        List<Integer> first = given().spec(requestSpec)
                .when().get(PAGE1_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);
        List<Integer> second = given().spec(requestSpec)
                .when().get(PAGE2_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);
        var allId = new ArrayList<>(first);
        allId.addAll(second);
        List<Integer> uniqueElements = allId.stream().distinct().collect(Collectors.toList());

        assertThat(allId, is(uniqueElements));
    }

    @Test
    void shouldGetFirstPageLikeDefault() {
        List<Integer> first = given().spec(requestSpec)
                .when().get(PAGE_DEFAULT)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);
        List<Integer> second = given().spec(requestSpec)
                .when().get(PAGE1)
                .then().spec(responseSpec).extract().jsonPath().getList(ID_PATH);

        assertThat(first, is(second));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/invalidPageNum.csv", numLinesToSkip = 1)
    void shouldGetErrorIfInvalidPageNum(String endPoint, String path, String error) {
        String message = given().spec(requestSpec)
                .when().get(endPoint)
                .then().statusCode(200).extract().jsonPath().getString(path);

        assertThat(message, is(error));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/validPageNum.csv", numLinesToSkip = 1)
    void shouldGetSuccessIfValidPageNum(String endPoint) {
        JsonPath page = given().spec(requestSpec)
                .when().get(endPoint)
                .then().spec(responseSpec).extract().jsonPath();

        assertThat(page, notNullValue());
    }

    @Test
    void shouldGetDefaultPageSize() {
        int items = given().spec(requestSpec)
                .when().get(PAGE_DEFAULT)
                .then().spec(responseSpec).extract().jsonPath().getList(ITEMS_PATH).size();

        assertThat(items, is(PAGE_SIZE));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/validSizePage.csv", numLinesToSkip = 1)
    void shouldGetSuccessIfValidSizePage(String endPoint, String path, int expected) {
        int size = given().spec(requestSpec)
                .when().get(endPoint)
                .then().spec(responseSpec).extract().jsonPath().getList(path).size();

        assertThat(size, is(expected));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/invalidSizePage.csv", numLinesToSkip = 1)
    void shouldGetErrorIfInvalidSizePage(String endPoint, String path, String error) {
        String message = given().spec(requestSpec)
                .when().get(endPoint)
                .then().statusCode(200).extract().jsonPath().getString(path);

        assertThat(message, is(error));
    }

    @Test
    void shouldGetAllCountryCodeByDefault() {
        List<String> first = given().spec(requestSpec)
                .when().get(PAGE1_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(COUNTRY_CODE_PATH);
        List<String> second = given().spec(requestSpec)
                .when().get(PAGE2_SIZE15)
                .then().spec(responseSpec).extract().jsonPath().getList(COUNTRY_CODE_PATH);
        var allCodes = new ArrayList<>(first);
        allCodes.addAll(second);

        assertThat(validCountryCode.containsAll(allCodes), is(true));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/validCountryCode.csv", numLinesToSkip = 1)
    void shouldGetSuccessIfValidCountryCode(String endPoint, String path, String expected) {
        List<String> codes = given().spec(requestSpec)
                .when().get(endPoint)
                .then().spec(responseSpec).extract().jsonPath().getList(path);
        var validateCodes = codes.stream().allMatch((s) -> s.contains(expected));

        assertThat(validateCodes, is(true));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/invalidCountryCode.csv", numLinesToSkip = 1)
    void shouldGetErrorIfInvalidCountryCode(String endPoint, String path, String error) {
        String message = given().spec(requestSpec)
                .when().get(endPoint)
                .then().statusCode(200).extract().jsonPath().getString(path);

        assertThat(message, is(error));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/validRegionNameFilter.csv", numLinesToSkip = 1)
    void shouldGetSuccessIfValidRegionFilter(String endPoint, String path, String expected) {
        String items = given().spec(requestSpec)
                .when().get(endPoint)
                .then().spec(responseSpec).extract().jsonPath().getString(path);

        assertThat(items, containsStringIgnoringCase(expected));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/invalidRegionNameFilter.csv", numLinesToSkip = 1)
    void shouldGetErrorIfInvalidRegionFilter(String endPoint, String path, String error) {
        String message = given().spec(requestSpec)
                .when().get(endPoint)
                .then().statusCode(200).extract().jsonPath().getString(path);

        assertThat(message, is(error));
    }

    @Test
    void shouldIgnoreFiltersIfRegionNameFilterActive() {
        List<String> first = given().spec(requestSpec)
                .when().get("/regions?q=рск&country_code=kz/page=2")
                .then().spec(responseSpec).extract().jsonPath().getList(COUNTRY_CODE_PATH);
        List<String> second = given().spec(requestSpec)
                .when().get("/regions?q=рск")
                .then().spec(responseSpec).extract().jsonPath().getList(COUNTRY_CODE_PATH);

        assertThat(first, is(second));
    }
}