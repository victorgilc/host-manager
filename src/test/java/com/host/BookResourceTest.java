package com.host;

import com.fasterxml.jackson.databind.JsonNode;
import com.host.utils.PayloadUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class BookResourceTest {

    public static final String LOCATION_HEADER = "Location";
    @Inject
    PayloadUtils payloadUtils;

    @ConfigProperty(name = "quarkus.http.test-port")
    int port;


    @SneakyThrows
    @ParameterizedTest
    @MethodSource("inputsPost")
    void givenInputs_whenCallPost_thenExecuteAsExpected(final Input input) {
        var validatable =
            given()
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .body(payloadUtils.getPayload(input.payloadPath))
            .when()
                .post(BookingResource.ROOT_PATH)
            .then()
                .statusCode(input.expectedStatus);


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
        given()
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(payloadUtils.getPayload(input.payloadPath))
        .when()
            .patch(BookingResource.ROOT_PATH)
        .then()
            .statusCode(input.expectedStatus);
    }

    public static Stream<Input> inputsPatch() {
        return Stream.of();
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
                            new ExpectedValidationMessagesPerPath("create.booking.start", "must not be null"),
                            new ExpectedValidationMessagesPerPath("create.booking.end", "must not be null"),
                            new ExpectedValidationMessagesPerPath("create.booking.guestName", "must not be null")
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
                            new ExpectedValidationMessagesPerPath("create.booking", "start date greater than end date")
                        )
                    )
                )
            .build()
        );
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


