package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.request.AlgorithmConfigRequest;
import vn.ttcs.vrp.dto.request.UpdateAlgorithmConfigRequest;
import vn.ttcs.vrp.dto.response.AlgorithmConfigResponse;

import java.util.List;

public interface AlgorithmConfigService {

    List<AlgorithmConfigResponse> findAll();

    AlgorithmConfigResponse create(AlgorithmConfigRequest request);

    AlgorithmConfigResponse update(Long id, UpdateAlgorithmConfigRequest request);
}
