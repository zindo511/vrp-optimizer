package vn.ttcs.vrp.service;

import vn.ttcs.vrp.model.DistanceMatrix;
import vn.ttcs.vrp.model.Location;

import java.util.List;

public interface DistanceMatrixService {

    /**
     *
     * @param locations danh sách địa chỉ các đơn hàng
     * @return kéo thứ còn về để bổ sung
     */
    List<DistanceMatrix> getDistanceMatrix(List<Location> locations);
}
