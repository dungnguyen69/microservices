package com.fullstack.Backend.dto.device;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
public class DropDownListsDTO {
	String[] itemTypeList;
	String[] statusList;
	String[] platformList;
	String[] ramList;
	String[] screenList;
	String[] storageList;
	String[] projectList;
	String[] originList;
}
