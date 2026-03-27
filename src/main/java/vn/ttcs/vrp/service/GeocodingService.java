package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.Coordinate;

public interface GeocodingService {

    /**
     *
     * @param address Địa chỉ định dạng dưới dạng văn bản
     * @return ooordinate chứa vĩ/kinh độ. Trả về null nếu không tìm thấy
     */
    Coordinate geocode(String address);

}
