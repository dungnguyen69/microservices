package com.fullstack.Backend.services;

import java.util.List;
import com.fullstack.Backend.models.Screen;
import com.fullstack.Backend.utils.dropdowns.ScreenList;

public interface ScreenService {
	public Screen findBySize(String size);
	public Boolean doesScreenExist(int id);

	public List<String> getScreenList();

	public List<ScreenList> fetchScreen();
}
