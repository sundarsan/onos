package org.onlab.onos.provider.of.device.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowSwitchListener;
import org.onlab.onos.of.controller.RoleState;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class OpenFlowDeviceProvider extends AbstractProvider implements DeviceProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private DeviceProviderService providerService;

    /**
     * Creates an OpenFlow device provider.
     */
    public OpenFlowDeviceProvider() {
        super(new ProviderId("org.onlab.onos.provider.of.device"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(new InternalDeviceProvider());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Device device) {
        log.info("Triggering probe on device {}", device.id());
    }

    @Override
    public void roleChanged(Device device, MastershipRole newRole) {
        switch (newRole) {
        case MASTER:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.MASTER);
            break;
        case STANDBY:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.EQUAL);
        case NONE:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.SLAVE);
            break;
        default:
            log.error("Unknown Mastership state : {}", newRole);

        }
        log.info("Accepting mastership role change for device {}", device.id());
    }

    private class InternalDeviceProvider implements OpenFlowSwitchListener {

        @Override
        public void switchAdded(final Dpid dpid) {
            URI uri = buildURI(dpid);
            providerService.deviceConnected(new DeviceId(uri), new DeviceDescription() {

                @Override
                public URI deviceURI() {
                    return buildURI(dpid);
                }
            });
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            URI uri = buildURI(dpid);
            providerService.deviceDisconnected(new DeviceId(uri));
        }

        private URI buildURI(Dpid dpid) {
            URI uri = null;
            try {
                uri = new URI("of", Long.toHexString(dpid.value()), null);
            } catch (URISyntaxException e) {
                log.warn("URI construction for device {} failed.", dpid);
            }
            return uri;
        }

    }

}
