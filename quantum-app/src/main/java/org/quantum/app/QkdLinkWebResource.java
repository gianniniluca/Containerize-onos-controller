/*
 * Copyright 2023-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quantum.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.Frequency;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.OchSignal;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.intent.*;
import org.onosproject.net.optical.util.OpticalIntentUtility;
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

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Quantum Links Management web resource.
 */
@Path("QkdLinks")
public class QkdLinkWebResource extends AbstractWebResource {

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

        for (QkdLink link: manager.getQkdLinks()) {
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

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQkdLink(@QueryParam("key") String key) {

        QkdLink link = manager.getQkdLink(key);

        //Reset Qkd node configuration

        //Remove optical intent
        intentService.withdraw(link.intent);

        return Response.ok().build();
    }

    /**
     * Ask for a new QKD link.
     *
     * @param srcConnectPoint
     * @param dstConnectPoint
     * @return
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestQkdLink(@QueryParam("srcConnectPoint") String srcConnectPoint,
                                   @QueryParam("dstConnectPoint") String dstConnectPoint)
    throws InterruptedException {

        ConnectPoint ingress = ConnectPoint.fromString(srcConnectPoint);
        ConnectPoint egress = ConnectPoint.fromString(dstConnectPoint);

        Key key = null;
        ApplicationId appId = nullIsNotFound(coreService.getAppId("org.quantum.app"), "App ID not found");
        boolean bidirectional = false;
        OchSignal signal = null;
        DefaultPath suggestedPath = null;

        //Ask a connectivity intent
        Intent intent = OpticalIntentUtility.createExplicitOpticalIntent(
                ingress,
                egress,
                deviceService,
                key,
                appId,
                bidirectional,
                signal,
                suggestedPath);

        intentService.submit(intent);

        long startTime = System.currentTimeMillis();
        while (intentService.getIntentState(intent.key()) != IntentState.INSTALLED) {
            log.info("Waiting for optical intent installation");
            Thread.sleep(10);





            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > 10000) {
                break;
            }
        }

        //Configure src and dst QKD
        QkdLink qkdLink = new QkdLink(
                getIntentId(intent),
                ingress,
                egress,
                getOchSignal(intent),
                5.0,
                intent);

        log.info("New QkdLink has been created {}", qkdLink);

        manager.addQkdLink(qkdLink.key, qkdLink);
        manager.configureQkdLinks(qkdLink.key);

        log.info("SOURCE configuration {}: ", manager.getQkdDeviceLinks(qkdLink.key));

        return Response.ok().build();
    }

    private OchSignal getOchSignal(Intent intent) {

        FlowRuleIntent installableIntent =
                (FlowRuleIntent) intentService.getInstallableIntents(intent.key())
                        .stream()
                        .filter(FlowRuleIntent.class::isInstance)
                        .findFirst()
                        .orElse(null);

        //FlowRules computed by the OpticalConnectivityIntentCompiler includes 3 criteria, one of those
        //is the OchSignal, thus retrieve used ochSignal from the selector of one of the installed rules
        if (installableIntent != null) {
            OchSignal signal = installableIntent.flowRules().stream()
                    .filter(r -> r.selector().criteria().size() == 3)
                    .map(r -> ((OchSignalCriterion)
                            r.selector().getCriterion(Criterion.Type.OCH_SIGID)).lambda())
                    .findFirst()
                    .orElse(null);

            return signal;
        } else {
            return null;
        }
    }

    private int getIntentId(Intent intent) {
        String string = intent.id().toString().split("x")[1];

        return Integer.valueOf(string);
    }
}
