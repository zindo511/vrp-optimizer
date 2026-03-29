package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.response.OsrmTableResponse;
import vn.ttcs.vrp.model.Location;

import java.util.List;

public interface OsrmClientService {
    OsrmTableResponse getDistanceMatrix(List<Location> locations);
}
