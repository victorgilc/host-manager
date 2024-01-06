package com.host;

import com.host.model.Booking;
import com.host.service.BookingService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;

@Path(BookingResource.ROOT_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    public static final String ID = "id";
    public static final String ID_PATH_PARAM = "/{id}";
    public static final String ROOT_PATH = "/booking";

    @Inject
    BookingService bookingService;

    @POST
    @Transactional
    public Response create(@Valid final Booking booking) {
        booking.persistAndFlush();
        final URI bookingUri = UriBuilder.fromPath(ROOT_PATH+"/"+booking.id).build();
        return Response.created(bookingUri).build();
    }

    @PATCH
    @Path(ID_PATH_PARAM)
    @Transactional
    public Response update(@PathParam(ID) final Long id,
                           final Booking toUpdateBooking) {
        bookingService.update(id, toUpdateBooking);
        return Response.ok().build();
    }

    @DELETE
    @Path(ID_PATH_PARAM)
    public void delete(@PathParam(ID) final Long id) {
        // Add your logic to delete a booking
    }

    @GET
    @Path(ID_PATH_PARAM)
    public Booking get(@PathParam(ID) Long id) {
        return Booking.findById(id);
    }
}
