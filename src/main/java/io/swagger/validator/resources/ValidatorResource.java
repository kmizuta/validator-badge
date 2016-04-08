package io.swagger.validator.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import io.swagger.validator.models.ValidationResponse;
import io.swagger.validator.services.OraValidatorService;
import io.swagger.validator.services.ValidatorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/")
@Api(value = "/validator", description = "Validator for Swagger Specs")
public class ValidatorResource {
    OraValidatorService service = new OraValidatorService();

    @GET
    @ApiOperation(value = "Validates a spec based on a URL")
    @ApiResponses(value = {})
    @Produces({"image/png"})
    public Response validateByUrl(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @ApiParam(value = "url of spec to validate") @QueryParam("url") String url) throws WebApplicationException {
        try {
            service.validateByUrl(request, response, url);
            response.addHeader("Cache-Control", "no-cache");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/debug")
    @Produces({"application/json"})
    @ApiOperation(value = "Validates a spec based on a URL",
            response = ValidationResponse.class,
            responseContainer = "List")
    @ApiResponses(value = {})
    public Response debugByUrl(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @ApiParam(value = "url of spec to validate") @QueryParam("url") String url) throws WebApplicationException {
        try {
            return Response.ok().entity(service.debugByUrl(request, response, url)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
    }

    @POST
    @Path("/debug")
    @Produces({"application/json"})
    @ApiOperation(value = "Validates a spec based on a URL",
            response = ValidationResponse.class,
            responseContainer = "List")
    @ApiResponses(value = {})
    public Response debugByContent(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @ApiParam(value = "spec contents") String spec) throws WebApplicationException {
        try {
            return Response.ok().entity(service.debugByContent(request, response, spec)).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }
    
    @GET
    @Path("/ora")
    @ApiOperation(value = "Validates a spec based on an API")
    @ApiResponses(value = {})
    @Produces({"image/png"})
    public Response validateByApi(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @ApiParam(value = "product to validate") @QueryParam("product") String product,
            @ApiParam(value = "level to validate") @QueryParam("level") String level,
            @ApiParam(value = "api to validate") @QueryParam("api") String api) throws WebApplicationException {
        try {
            service.validateByApi(request, response, product, api, level);
            response.addHeader("Cache-Control", "no-cache");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }
    
    @GET
    @Path("/oradebug")
    @Produces({"application/json"})
    @ApiOperation(value = "Validates a spec based on an API",
            response = ValidationResponse.class,
            responseContainer = "List")
    @ApiResponses(value = {})
    public Response debugByApi(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @ApiParam(value = "product to validate") @QueryParam("product") String product,
            @ApiParam(value = "level to validate") @QueryParam("level") String level,
            @ApiParam(value = "api to validate") @QueryParam("api") String api) throws WebApplicationException {
        try {
            return Response.ok().entity(service.debugByApi(product, api, level)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
    }

}