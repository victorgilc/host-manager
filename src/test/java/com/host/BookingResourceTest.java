package com.host;

import com.fasterxml.jackson.databind.JsonNode;
import com.host.exception.OverlapMessageWrapper;
import com.host.exception.PropertyAlreadyBookedException;
import com.host.model.Booking;
import com.host.utils.PayloadUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Method;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

@QuarkusTest
public class BookingResourceTest {
    public static final String START_VERSION_PAYLOAD = "start_version";
    public static final String UPDATE_VERSION_PAYLOAD = "update_version";
    public static final String LOCATION_HEADER = "Location";
    public static final String CANCELED_PAYLOAD_PROPERTY = "canceled";
    public static final String INITIAL_URL_LOCALHOST = "http://localhost:";
    public static final String MESSAGE_RESPONSE_PROPERTY = "message";

    @Inject
    PayloadUtils payloadUtils;
    @ConfigProperty(name= "quarkus.http.test-port")
    int port;

    @BeforeEach
    @Transactional
    public void setup(){
        Booking.deleteAll();
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("inputsPost")
    void givenInputs_whenCallPost_thenExecuteAsExpected(final Input input) {
        var payload = payloadUtils.getPayload(input.payloadPath).toString();
        var validatable = executePost(payload, input.expectedStatus);

        if(input.expectedTriggeredValidations.isPresent()){
           var response = validatable.extract().body().as(JsonNode.class);
           checkViolations(response, input.expectedTriggeredValidations.get());
        }
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("inputsPatch")
    void givenInputs_whenCallPatch_thenExecuteAsExpected(final Input input) {
        var payload = payloadUtils.getPayload(input.payloadPath);
        var startPayload = payload.get(START_VERSION_PAYLOAD);
        var updatePayload = payload.get(UPDATE_VERSION_PAYLOAD);

        var validatablePost = executePost(startPayload.toString(), HttpStatus.SC_CREATED);
        var location = validatablePost.extract().header(LOCATION_HEADER);
        var booking = getBooking(location);
        var validatablePatch = executePatch(input.expectedStatus, updatePayload, booking);

        if(input.expectedTriggeredValidations.isPresent()){
            var response = validatablePatch.extract().body().as(JsonNode.class);
            checkViolations(response, input.expectedTriggeredValidations.get());
        } else {
            var bookingAfterUpdate = getBooking(location);
            var endUpdatePayload = updatePayload.get(Booking.END);
            var startUpdatePayload =  updatePayload.get(Booking.START);
            var personIdUpdatePayload = updatePayload.get(Booking.PERSON_ID);
            var canceledUpdatePayload = updatePayload.get(CANCELED_PAYLOAD_PROPERTY);
            var endInitialPayload = startPayload.get(Booking.END);
            var startInitialPayload = startPayload.get(Booking.START);
            var personIdInitialPayload = startPayload.get(Booking.PERSON_ID);

            Assertions.assertAll(
                ()->Assertions.assertEquals(bookingAfterUpdate.end.toString(),
                    Objects.nonNull(endUpdatePayload)? endUpdatePayload.textValue() :
                        endInitialPayload.textValue()),

                ()->Assertions.assertEquals(bookingAfterUpdate.start.toString(),
                    Objects.nonNull(startUpdatePayload) ? startUpdatePayload.textValue() :
                        startInitialPayload.textValue()),

                ()->Assertions.assertEquals(bookingAfterUpdate.personId,
                    Objects.nonNull(personIdUpdatePayload) ? personIdUpdatePayload.longValue() :
                        personIdInitialPayload.longValue()),

                ()->Assertions.assertEquals(bookingAfterUpdate.canceled,
                    Objects.nonNull(canceledUpdatePayload) ? canceledUpdatePayload.toString() :
                        false)
            );
        }
    }
    @ParameterizedTest
    @MethodSource("methodsWithNonExistentId")
    public void givenNonExistentBooking_whenCallMethod_thenBadRequest(final Method method){
        final String nonExistentId = "123";

        var body =
            given()
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .when()
                .request(method, INITIAL_URL_LOCALHOST +port+ BookingResource.ROOT_PATH+nonExistentId)
            .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST).extract().body().as(JsonNode.class);

        Assertions.assertEquals("Resource with id '"+nonExistentId+"' doesn't exist",
                body.get("message").textValue());
    }

    @Test
    public void givenExistentBooking_whenCallDelete_thenSucceed(){
        var validatablePost = executePost(payloadUtils.getPayload("/bookings/valid_booking.json")
            .toString(), HttpStatus.SC_CREATED);
        var location = validatablePost.extract().header(LOCATION_HEADER);
        when()
            .delete(location)
        .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void givenExistentBooking_whenCallPost_thenBadRequestDueOverlap(){
        var payloads = payloadUtils.getPayload("/bookings/booking_create_overlap_validation.json");
        var initialBook = payloads.get("initial_booking");
        executePost(initialBook.toString(), HttpStatus.SC_CREATED);
        verifyOverlap(payloads.get("end_date_inside"), initialBook);
        verifyOverlap(payloads.get("range_inside"), initialBook);
        verifyOverlap(payloads.get("start_date_inside"), initialBook);
        verifyOverlap(payloads.get("over_start_and_end"), initialBook);
    }

    @Test
    public void givenExistentBooking_whenCallPatch_thenVerifySameBookingAndActProperly(){
        var payloads = payloadUtils.getPayload("/bookings/booking_create_overlap_validation.json");
        var initialBook = payloads.get("initial_booking");
        var validatablePost = executePost(initialBook.toString(), HttpStatus.SC_CREATED);
        var location = validatablePost.extract().header(LOCATION_HEADER);

        executePatch(HttpStatus.SC_NO_CONTENT, payloads.get("end_date_inside"), location);
        executePatch(HttpStatus.SC_NO_CONTENT, payloads.get("range_inside"), location);
        executePatch(HttpStatus.SC_NO_CONTENT, payloads.get("start_date_inside"), location);
        executePatch(HttpStatus.SC_NO_CONTENT, payloads.get("over_start_and_end"), location);
    }

    @Test
    public void givenExistentBooking_whenCallPatch_thenVerifyDiffBookingAndActProperly(){
        var payloads = payloadUtils.getPayload("/bookings/booking_update_overlap_validation_diff_booking.json");

        var rangeInside = payloads.get("range_inside");
        var initialBook = payloads.get("initial_booking");
        var anotherBooking = payloads.get("another_booking");

        var validatablePost = executePost(initialBook.toString(), HttpStatus.SC_CREATED);
        var location = validatablePost.extract().header(LOCATION_HEADER);
        executePost(anotherBooking.toString(), HttpStatus.SC_CREATED);
        executePatch(HttpStatus.SC_BAD_REQUEST, rangeInside, location);
    }

    @Test
    public void givenExistentBooking_whenCallPatch_thenVerifyCanceledBookingAndActProperly(){
        var payloads = payloadUtils.getPayload("/bookings/booking_update_overlap_validation_canceled_booking.json");

        var rangeInside = payloads.get("range_inside");
        var initialBook = payloads.get("initial_booking");
        var anotherBooking = payloads.get("another_booking");

        var validatablePost = executePost(initialBook.toString(), HttpStatus.SC_CREATED);
        var location = validatablePost.extract().header(LOCATION_HEADER);
        executePost(anotherBooking.toString(), HttpStatus.SC_CREATED);
        executePatch(HttpStatus.SC_NO_CONTENT, rangeInside, location);
    }

    @Test
    public void givenBookingsWithDaysCloseEachOther_whenCallPost_thenSucceed(){
        var payloads = payloadUtils.getPayload("/bookings/valid_booking_days_close.json");
        payloads.forEach(p->executePost(p.toString(), HttpStatus.SC_CREATED));
    }

    public static Stream<Method> methodsWithNonExistentId() {
        return Stream.of(
            Method.DELETE,
            Method.PATCH,
            Method.GET
        );
    }

    public static Stream<Input> inputsPatch() {
        return Stream.of(
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/valid_booking_update_dates_and_person_id.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/booking_update_just_start.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/booking_update_just_end.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/booking_update_just_person_id.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/booking_update_and_cancel.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/rebook_canceled_booking.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_BAD_REQUEST)
                .payloadPath("/bookings/booking_update_start_after_end.json")
                .expectedTriggeredValidations(Optional.of(
                    List.of(new ExpectedValidationMessagesPerPath("create.booking",
                            "start date greater than end date"))
                ))
            .build()
        );
    }

    public static Stream<Input> inputsPost() {
        return Stream.of(
            Input.builder()
                .expectedStatus(HttpStatus.SC_CREATED)
                .payloadPath("/bookings/valid_booking.json")
                .expectedTriggeredValidations(Optional.empty())
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_BAD_REQUEST)
                .payloadPath("/bookings/empty.json")
                .expectedTriggeredValidations(
                    Optional.of(
                        List.of(
                            new ExpectedValidationMessagesPerPath("create.booking.start",
                                    "must not be null"),
                            new ExpectedValidationMessagesPerPath("create.booking.end",
                                    "must not be null"),
                            new ExpectedValidationMessagesPerPath("create.booking.personId",
                                    "must not be null")
                        )
                    )
                )
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_BAD_REQUEST)
                .payloadPath("/bookings/booking_start_date_greater_end_date.json")
                .expectedTriggeredValidations(
                    Optional.of(
                        List.of(
                            new ExpectedValidationMessagesPerPath("create.booking",
                                    "start date greater than end date")
                        )
                    )
                )
            .build()
        );
    }

    private ValidatableResponse executePost(final String payload, final int expectedStatus) {
         return
             given()
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .body(payload)
             .when()
                .post(BookingResource.ROOT_PATH)
             .then()
                .statusCode(expectedStatus);
    }

    private static ValidatableResponse executePatch(final int expectedStatus, final JsonNode updatePayload, final String location) {
        return
            given()
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .body(updatePayload.toString())
            .when()
                .patch(location)
            .then()
                .statusCode(expectedStatus);
    }

    private static ValidatableResponse executePatch(final int expectedStatus, final JsonNode updatePayload, final Booking booking) {
        return executePatch(expectedStatus, updatePayload, BookingResource.ROOT_PATH+ booking.id);
    }


    private void verifyOverlap(final JsonNode bookTry, final JsonNode initialBook) {
        var validatable = executePost(bookTry.toString(), HttpStatus.SC_BAD_REQUEST);
        var body = validatable.extract().body().as(JsonNode.class);

        Assertions.assertAll(
                ()->Assertions.assertEquals(body.get(MESSAGE_RESPONSE_PROPERTY).textValue(),
                        PropertyAlreadyBookedException.ERROR_MESSAGE),
                ()->Assertions.assertEquals(body.get(OverlapMessageWrapper.START_TRY_TO_BOOK), bookTry.get("start")),
                ()->Assertions.assertEquals(body.get(OverlapMessageWrapper.END_TRY_TO_BOOK), bookTry.get("end")),
                ()->Assertions.assertEquals(body.get(OverlapMessageWrapper.START_BOOKED), initialBook.get("start")),
                ()->Assertions.assertEquals(body.get(OverlapMessageWrapper.END_BOOKED), initialBook.get("end"))
        );
    }


    private Booking getBooking(final String url) {
        return
            given()
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .when()
                .get(url)
            .then()
                .extract().body().as(Booking.class);
    }

    private static void checkViolations(final JsonNode response,
                                        final List<ExpectedValidationMessagesPerPath> expectedValidationMessagesPerPath) {
        final List<JsonNode> violations = new ArrayList<>();
        for (JsonNode node :  response.get("parameterViolations")) {
            violations.add(node);
        }

        Assertions.assertAll(
            () -> expectedValidationMessagesPerPath.forEach(e ->
                Assertions.assertTrue(violations.stream().anyMatch(i ->
                    e.path.equals(i.get("path").asText()) && e.message.equals(i.get(MESSAGE_RESPONSE_PROPERTY).asText())
            )))
        );
    }

    @Builder
    @ToString(of = {"expectedStatus", "payloadPath"})
    static class Input {
        int expectedStatus;
        String payloadPath;
        Optional<List<ExpectedValidationMessagesPerPath>> expectedTriggeredValidations;
    }

    @AllArgsConstructor
    static class ExpectedValidationMessagesPerPath{
        String path;
        String message;
    }
}


