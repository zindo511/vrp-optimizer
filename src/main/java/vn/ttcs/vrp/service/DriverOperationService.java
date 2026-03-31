package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.response.MyRouteResponse;

public interface DriverOperationService {

    // API: tài xế lấy lộ trình hôm nay
    MyRouteResponse getMyTodayRoutes();
}
