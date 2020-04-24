package io.weatherStation;

import io.weatherStation.dto.StationDto;
import io.weatherStation.manager.StationManager;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/stations")
public class StationResource {
    private StationManager stationManager;

    @Inject
    public StationResource(StationManager stationManager) {
        this.stationManager = stationManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<StationDto> get(){
        return stationManager.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public StationDto post(StationDto stationDto){
        return stationManager.add(stationDto);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public StationDto put(StationDto stationDto){
        return stationManager.update(stationDto);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public StationDto getById(@PathParam("id") Long id){
        return stationManager.findById(id);
    }
}
