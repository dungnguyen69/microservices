package com.fullstack.Backend.responses.device;

import com.fullstack.Backend.models.Device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddDeviceResponse {
	Device newDevice;
	Boolean isAddedSuccessful;
}
