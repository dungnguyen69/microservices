package com.fullstack.Backend.responses.device;

import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.utils.ErrorMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddDeviceResponse {
	Device newDevice;
	Boolean isAddedSuccessful;
	List<ErrorMessage> errorMessages;

	public AddDeviceResponse(List<ErrorMessage> errors) {
		this.errorMessages = errors;
	}
}
