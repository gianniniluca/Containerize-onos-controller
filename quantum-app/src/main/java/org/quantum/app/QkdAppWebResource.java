package org.quantum.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Quantum Apps Management web resource.
 */
@Path("QkdApps")
public class QkdAppWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    IntentService intentService = get(IntentService.class);
    DeviceService deviceService = get(DeviceService.class);
    CoreService coreService = get(CoreService.class);
    QkdAppManager manager = get(QkdAppManager.class);

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @GET
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQkdLinks() {

        ArrayNode links = mapper().createArrayNode();

        for (QkdApp app : manager.getQkdApps()) {
            ObjectNode node = mapper().createObjectNode()
                    .put("sae_id", app.saeId)
                    .put("location", app.location.id().toString());

            links.add(node);
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("QkdApps", links);
        return ok(root).build();
    }

    /**
     * Explicit app registration.
     *
     * @param appAddress
     * @param appLocation
     * @return
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response implicitRegistration(@QueryParam("appAddress") String appAddress,
                                   @QueryParam("appLocation") String appLocation) {

        return Response.ok()
                .header("SAE_ID", 22)
                .build();
    }

    /**
     * Open Key Session.
     * Implicit app registration.
     *
     * @param appAddress
     * @param appLocation
     * @return
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response openKeySession(@QueryParam("appAddress") String appAddress,
                                   @QueryParam("appLocation") String appLocation) {

        return Response.ok()
                .header("SAE_ID", 22)
                .header("APP_ID", 44)
                .build();
    }
}

