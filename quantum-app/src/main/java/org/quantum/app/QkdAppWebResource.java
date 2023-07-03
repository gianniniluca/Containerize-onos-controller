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

@Path("QkdApps")
public class QkdAppWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    IntentService intentService = get(IntentService.class);
    DeviceService deviceService = get(DeviceService.class);
    CoreService coreService = get(CoreService.class);
    QkdLinkManager manager = get(QkdLinkManager.class);

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

        for (QkdLink link : manager.getQkdLinks()) {
            ObjectNode node = mapper().createObjectNode()
                    .put("id", link.id)
                    .put("src", link.src.toString())
                    .put("dst", link.dst.toString())
                    .put("signal", link.signal.spacingMultiplier())
                    .put("attenuation", link.attenuation)
                    .put("key", link.key)
                    .put("intent-id", link.intent.id().toString());

            links.add(node);
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("QkdLinks", links);
        return ok(root).build();
    }
}

