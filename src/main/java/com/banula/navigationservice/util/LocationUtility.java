package com.banula.navigationservice.util;

import com.banula.openlib.ocpi.model.Location;
import com.banula.openlib.ocpi.model.vo.Connector;
import com.banula.openlib.ocpi.model.vo.EVSE;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class LocationUtility {

    @Nullable
    public static Connector findConnectorInEvseByConnectorId(String connectorId, EVSE evse) {
        if (evse.getConnectors() == null || evse.getConnectors().isEmpty()) {
            return null;
        }
        return evse.getConnectors().stream()
                .filter(connectorIteration -> connectorIteration.getId().equals(connectorId))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static EVSE findEvseInLocationByEvseUid(String evseUid, Location location) {
        if (location.getEvses() == null || location.getEvses().isEmpty()) {
            return null;
        }
        return location.getEvses().stream()
                .filter(evseIteration -> evseIteration.getUid().equals(evseUid))
                .findFirst()
                .orElse(null);
    }

    public static int evseIndexOf(List<EVSE> evses, EVSE evse) {
        for (int i = 0; i < evses.size(); i++) {
            if (evses.get(i).getUid().equals(evse.getUid())) {
                return i;
            }
        }
        return -1;
    }

    public static int connectorIndexOf(List<Connector> connectors, Connector connector) {
        for (int i = 0; i < connectors.size(); i++) {
            if (connectors.get(i).getId().equals(connector.getId())) {
                return i;
            }
        }
        return -1;
    }

}
