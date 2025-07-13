package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.model.SerialNumber;

import java.util.Comparator;
import java.util.List;

public class AscendingOrderTargetDeviceResolver implements TargetDeviceResolver {

    @Override
    public SerialNumber resolveTargetDevice(List<SerialNumber> serialNumberList) {
        // sort by comparing serial number to string and return first
        return serialNumberList.stream()
                .sorted(Comparator.comparing(SerialNumber::toString))
                .findFirst().get();
    }
}
