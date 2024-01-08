package com.host;

import com.fasterxml.jackson.databind.JsonNode;
import com.host.model.Booking;
import com.host.utils.PayloadUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class BookingResourceTest {
    public static final String START_VERSION_PAYLOAD = "start_version";
    public static final String UPDATE_VERSION_PAYLOAD = "update_version";
    public static final String LOCATION_HEADER = "Location";
    public static final String END_PAYLOAD_PROPERTY = "end";
    public static final String START_PAYLOAD_PROPERTY = "start";
    public static final String GUEST_NAME_PAYLOAD_PROPERTY = "guest_name";
    @Inject
    PayloadUtils payloadUtils;
    @ConfigProperty(name = "quarkus.http.test-port")
    int port;

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("inputsPost")
    void givenInputs_whenCallPost_thenExecuteAsExpected(final Input input) {
        var payload = payloadUtils.getPayload(input.payloadPath).toString();
        var validatable = executePost(payload, input.expectedStatus);

        if(HttpStatus.SC_CREATED == input.expectedStatus){
            validatable.header(LOCATION_HEADER,"http://localhost:"+port+"/booking/1");
            return;
        }

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

        var validatablePatch = given()
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(updatePayload.toString())
        .when()
            .patch(BookingResource.ROOT_PATH+ "/"+booking.id)
        .then()
            .statusCode(input.expectedStatus);

        if(input.expectedTriggeredValidations.isPresent()){
            var response = validatablePatch.extract().body().as(JsonNode.class);
            checkViolations(response, input.expectedTriggeredValidations.get());
        } else {
            var bookingAfterUpdate = getBooking(location);
            var endUpdatePayload = updatePayload.get(END_PAYLOAD_PROPERTY);
            var startUpdatePayload =  updatePayload.get(START_PAYLOAD_PROPERTY);
            var guestUpdatePayload = updatePayload.get(GUEST_NAME_PAYLOAD_PROPERTY);
            var endInitialPayload = startPayload.get(END_PAYLOAD_PROPERTY);
            var startInitialPayload = startPayload.get(START_PAYLOAD_PROPERTY);
            var guestInitialPayload = startPayload.get(GUEST_NAME_PAYLOAD_PROPERTY);

            Assertions.assertAll(
                ()->Assertions.assertEquals(bookingAfterUpdate.end.toString(),
                    Objects.nonNull(endUpdatePayload)? endUpdatePayload.textValue() :
                            endInitialPayload.textValue()),

                ()->Assertions.assertEquals(bookingAfterUpdate.start.toString(),
                    Objects.nonNull(startUpdatePayload) ? startUpdatePayload.textValue() :
                            startInitialPayload.textValue()),

                ()->Assertions.assertEquals(bookingAfterUpdate.guestName,
                    Objects.nonNull(guestUpdatePayload) ? guestUpdatePayload.textValue() :
                            guestInitialPayload.textValue())
            );
        }
    }

    public static Stream<Input> inputsPatch() {
        return Stream.of(
            Input.builder()
                .expectedStatus(HttpStatus.SC_NO_CONTENT)
                .payloadPath("/bookings/valid_booking_update_dates_and_guest_name.json")
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
                .payloadPath("/bookings/booking_update_just_guest.json")
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
                            new ExpectedValidationMessagesPerPath("create.booking.guestName",
                                    "must not be null")
                        )
                    )
                )
            .build(),
            Input.builder()
                .expectedStatus(HttpStatus.SC_BAD_REQUEST)
                .payloadPath("/bookings/booking_end_date_greater_start_date.json")
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
                    e.path.equals(i.get("path").asText()) && e.message.equals(i.get("message").asText())
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


