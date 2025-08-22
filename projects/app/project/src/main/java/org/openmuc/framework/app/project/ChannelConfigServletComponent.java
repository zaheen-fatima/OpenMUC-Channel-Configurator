package org.openmuc.framework.app.project;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


@Component(immediate = true)
public class ChannelConfigServletComponent {
    private static final Logger logger = LoggerFactory.getLogger(ChannelConfigServletComponent.class);

    @Reference
    private HttpService httpService;

    @Activate
    public void activate() {
        try {
            httpService.registerServlet("/config", new ChannelConfigServlet(), null, null);
            logger.info("ChannelConfigServlet registered at /config");
        } catch (Exception e) {
            logger.error("Failed to register servlet", e);
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            httpService.unregister("/config");
            logger.info("ChannelConfigServlet unregistered");
        } catch (Exception e) {
            logger.error("Failed to unregister servlet", e);
        }
    }


    public static class ChannelConfigServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html");
            try (InputStream is = getClass().getResourceAsStream("/web/form.html")) {
                if (is == null) {
                    resp.getWriter().println("Form not found!");
                    return;
                }
                is.transferTo(resp.getOutputStream());
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                String deviceId = req.getParameter("deviceId");
                String driver = req.getParameter("driver");

                String[] channelIds = req.getParameterValues("channelId");
                String[] unitIds = req.getParameterValues("unitId");
                String[] primaryTables = req.getParameterValues("primaryTable");
                String[] addresses = req.getParameterValues("address");
                String[] datatypes = req.getParameterValues("datatype");

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.newDocument();

                // === Root <configuration> instead of <channels> ===
                Element configEl = doc.createElement("configuration");
                doc.appendChild(configEl);

                // <driver id="modbus">
                Element driverEl = doc.createElement("driver");
                driverEl.setAttribute("id", "modbus");
                configEl.appendChild(driverEl);

                // <device ...>
                Element deviceEl = doc.createElement("device");
                deviceEl.setAttribute("id", deviceId);
                driverEl.appendChild(deviceEl);

                if ("modbus-tcp".equals(driver)) {
                    String host = req.getParameter("host");
                    String port = req.getParameter("port");

                    Element deviceAddress = doc.createElement("deviceAddress");
                    deviceAddress.setTextContent(host + ":" + port);
                    deviceEl.appendChild(deviceAddress);

                    Element settingsEl = doc.createElement("settings");
                    settingsEl.setTextContent("TCP");
                    deviceEl.appendChild(settingsEl);

                } else if ("modbus-rtu".equals(driver)) {
                    String portName = req.getParameter("portName");
                    String baudRate = req.getParameter("baudRate");
                    String dataBits = req.getParameter("dataBits");
                    String parity = req.getParameter("parity");
                    String stopBits = req.getParameter("stopBits");
                    String echo = req.getParameter("echo");
                    String flowIn = req.getParameter("flowIn");
                    String flowOut = req.getParameter("flowOut");

                    Element deviceAddress = doc.createElement("deviceAddress");
                    deviceAddress.setTextContent(portName);
                    deviceEl.appendChild(deviceAddress);

                    Element settingsEl = doc.createElement("settings");
                    settingsEl.setTextContent(
                            "RTU:SERIAL_ENCODING_RTU:" + baudRate +
                                    ":DATABITS_" + dataBits +
                                    ":PARITY_" + parity +
                                    ":STOPBITS_" + stopBits +
                                    ":ECHO_" + echo.toUpperCase() +
                                    ":FLOWCONTROL_" + flowIn +
                                    ":FLOWCONTROL_" + flowOut
                    );
                    deviceEl.appendChild(settingsEl);
                }

                if (channelIds != null) {
                    for (int i = 0; i < channelIds.length; i++) {
                        Element channelEl = doc.createElement("channel");
                        channelEl.setAttribute("id", channelIds[i]);

                        Element addrEl = doc.createElement("channelAddress");
                        addrEl.setTextContent(
                                unitIds[i] + ":" + primaryTables[i] + ":" + addresses[i] + ":" + datatypes[i]
                        );
                        channelEl.appendChild(addrEl);

                        // Add <valueType>
                        Element valueTypeEl = doc.createElement("valueType");
                        valueTypeEl.setTextContent(datatypes[i]);
                        channelEl.appendChild(valueTypeEl);

                        // Add default sampling + logging intervals
                        Element sampEl = doc.createElement("samplingInterval");
                        sampEl.setTextContent("1000");
                        channelEl.appendChild(sampEl);

                        Element logEl = doc.createElement("loggingInterval");
                        logEl.setTextContent("4000");
                        channelEl.appendChild(logEl);

                        deviceEl.appendChild(channelEl);
                    }
                }

                String userDir = System.getProperty("user.dir");

                File dir = new File(userDir, "conf");

                if (!dir.exists()) {
                    dir.mkdirs();
                    logger.info("Created directory: " + dir.getAbsolutePath());
                }

                File file = new File(dir, "channels.xml");

                if (file.exists()) {
                    logger.info("channels.xml exists. Will update contents.");

                } else {
                    boolean created = file.createNewFile();
                    if (created) {
                        logger.info("channels.xml not found. A new one was created at: " + file.getAbsolutePath());
                    } else {
                        logger.warn("Failed to create channels.xml");
                    }
                }


                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(doc), new StreamResult(file));

                resp.setContentType("text/html");
                resp.getWriter().println("<h2>channels.xml generated successfully!</h2>");
                resp.getWriter().println("<p>Saved at: " + file.getAbsolutePath() + "</p>");
                resp.getWriter().println("<a href='/config'>Go back</a>");

            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain");
                e.printStackTrace(resp.getWriter());
            }
        }


    }
}
