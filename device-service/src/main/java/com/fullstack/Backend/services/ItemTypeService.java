package com.fullstack.Backend.services;

import java.util.List;

import com.fullstack.Backend.models.ItemType;
import com.fullstack.Backend.utils.dropdowns.ItemTypeList;

public interface ItemTypeService {
	public ItemType findByName(String name);
	public Boolean doesItemTypeExist(int id);

	public List<String> getItemTypeList();

	public List<ItemTypeList> fetchItemTypes();
}
