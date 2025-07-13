package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.model.SerialNumber;

import java.util.List;

public interface TargetDeviceResolver {

    SerialNumber resolveTargetDevice(List<SerialNumber> serialNumberList);
}
