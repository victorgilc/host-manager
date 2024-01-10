package com.host;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.host.exception.*;
import com.host.model.Booking;
import com.host.service.BookingService;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Path(BookingResource.ROOT_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    public static final String ID = "id";
    public static final String ID_PATH_PARAM = "{id}";
    public static final String ROOT_PATH = "/booking/";
    @Inject
    BookingService bookingService;
    @Inject
    ObjectMapper objectMapper;

    @POST
    public Response create(@Valid final Booking booking) {
        try{
            bookingService.create(booking);
            final URI bookingUri = UriBuilder.fromPath(ROOT_PATH+booking.id).build();
            return Response.created(bookingUri).build();
        } catch(final PropertyAlreadyBookedException e){
            return returnBadRequestForProperyAlreadyBooked(e);
        }
    }

    @SneakyThrows
    @PATCH
    @Path(ID_PATH_PARAM)
    public Response update(@PathParam(ID) final Long id,
                           final Booking toUpdateBooking) {
        try {
          bookingService.update(id, toUpdateBooking);
        } catch(final PropertyAlreadyBookedException e){
            return returnBadRequestForProperyAlreadyBooked(e);
        }  catch(final ResourceDoesNotExistException e){
            return returnBadRequestWithExceptionMessage(e);
        } catch(final RuntimeException e){
            if(e.getCause()!=null && e.getCause().getCause() instanceof ConstraintViolationException constraintViolation){
                final Set<ConstraintViolation<?>> violations = constraintViolation.getConstraintViolations();
                final ConstraintViolationStructure constraintViolationStructure = new ConstraintViolationStructure();
                final List<ConstraintViolationStructure.ParameterViolation> parameterViolations = new ArrayList<>();
                for (ConstraintViolation<?> violation : violations) {
                    parameterViolations.add(new ConstraintViolationStructure.ParameterViolation(violation));
                }
                constraintViolationStructure.parameterViolations = parameterViolations;
                var violationsJson = objectMapper.convertValue(constraintViolationStructure, JsonNode.class);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(violationsJson)
                    .build();
            }
        }
        return Response.noContent().build();
    }

    private static Response returnBadRequestForProperyAlreadyBooked(PropertyAlreadyBookedException e) {
        var overlapMessageWrapper = OverlapMessageWrapper.builder()
                .message(e.getMessage())
                .endBooked(e.alreadyBooked.end)
                .startBooked(e.alreadyBooked.start)
                .startTryToBook(e.triedToBook.start)
                .endTryToBook(e.triedToBook.end)
                .build();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(overlapMessageWrapper)
                .build();
    }

    @DELETE
    @Path(ID_PATH_PARAM)
    public Response delete(@PathParam(ID) final Long id) {
        try{
            bookingService.remove(id);
            return Response.noContent().build();
        }  catch(final ResourceDoesNotExistException e){
            return returnBadRequestWithExceptionMessage(e);
        }
    }

    @GET
    @Path(ID_PATH_PARAM)
    public Response get(@PathParam(ID) Long id) {
        try{
            var booking = bookingService.get(id);
            return Response.status(Response.Status.OK).entity(booking)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
        }  catch(final ResourceDoesNotExistException e){
            return returnBadRequestWithExceptionMessage(e);
        }
    }

    private static Response returnBadRequestWithExceptionMessage(
            final RuntimeException e) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new MessageWrapper(e.getMessage()))
            .build();
    }
}
