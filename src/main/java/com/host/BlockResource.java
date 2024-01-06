package com.host;

import com.host.model.Block;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import static com.host.BlockResource.ROOT_PATH;

@Path(ROOT_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BlockResource {

    public static final String ID_PATH_PARAM = "/{id}";
    public static final String ROOT_PATH = "/block";

    @POST
    @Transactional
    public void create(final Block block) {
        block.persistAndFlush();
    }

    @PATCH
    @Transactional
    public void update(final Block block) {
        block.persistAndFlush();

    }

    @DELETE
    @Path(ID_PATH_PARAM)
    public void delete(@PathParam("id") final Long id) {
        Block.deleteById(id);
    }

    @GET
    @Path("/{id}")
    public Block get(@PathParam("id") Long id) {
        return Block.findById(id);
    }
}
