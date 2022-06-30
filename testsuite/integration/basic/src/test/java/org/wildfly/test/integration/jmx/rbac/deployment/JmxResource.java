/*
 *
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.wildfly.test.integration.jmx.rbac.deployment;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class JmxResource {
    private static final String DATASOURCE_MBEAN_NAME = "jboss.as:subsystem=datasources,data-source=ExampleDS,statistics=pool";
    private static final String ROOT_MBEAN_NAME = "jboss.as:management-root=server";

    String HOST_NAME = "127.0.0.1";
    String PORT = "9990";


    @GET
    @Path("/platform")
    public Response usePlatformMBeanServer() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            return callMBeanServer(server);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GET
    @Path("/found")
    public Response useFoundMBeanServer() throws Exception {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);

        Response response = null;
        for (int i = 0; i < servers.size(); i++) {
            MBeanServer server = servers.get(i);

            if (server.isRegistered(ObjectName.getInstance(DATASOURCE_MBEAN_NAME))) {
                response = callMBeanServer(server);
            }
        }

        if (response == null) {
            throw new IllegalStateException("No MBeanServer found");
        }
        return response;
    }

    @GET
    @Path("/remote")
    public Response useRemoteMBeanServer() throws Exception {
        JMXServiceURL jmx_service_url = new JMXServiceURL("service:jmx:remote+http://" + HOST_NAME + ":" + PORT);
        Map<String,Object> environment = new HashMap<String,Object>();
        JMXConnector connector = JMXConnectorFactory.connect(jmx_service_url, null);
        MBeanServerConnection conn = connector.getMBeanServerConnection();
        try {
            return callMBeanServer(conn);
        } finally {
            connector.close();
        }
    }

    private Response callMBeanServer(MBeanServerConnection server) {
        try {
            server.getAttribute(new ObjectName(DATASOURCE_MBEAN_NAME), "ActiveCount");
            Object whoAmI = server.invoke(new ObjectName(ROOT_MBEAN_NAME), "whoami", new Object[]{Boolean.TRUE}, new String[]{"verbose"});
            System.out.println("=======================================");
            System.out.println(whoAmI.getClass());
            System.out.println(whoAmI);
            System.out.println("=======================================");

            return Response.ok(whoAmI.toString()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
