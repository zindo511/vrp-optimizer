package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.request.LocationUpdateRequest;
import vn.ttcs.vrp.dto.request.StopStatusRequest;
import vn.ttcs.vrp.dto.response.MyRouteResponse;

public interface DriverOperationService {

    MyRouteResponse getMyTodayRoutes();

    void updateStopStatus(Long id, StopStatusRequest request);

    void updateMyLocation(LocationUpdateRequest request);
}
