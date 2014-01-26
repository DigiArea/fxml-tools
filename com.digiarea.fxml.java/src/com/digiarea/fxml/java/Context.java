package com.digiarea.fxml.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dagxp.core.utils.StringUtils;
import com.dagxp.lmm.jse.ClassOrInterfaceType;
import com.dagxp.lmm.jse.NameExpr;
import com.dagxp.lmm.jse.utils.NodeUtils;

public class Context {

	private List<String> names = new ArrayList<>();

	public String getNewIdentifier(String name) {
		NameExpr nameExpr = NodeUtils.createNameExpr(name);
		String newId = StringUtils.firstToLower(nameExpr.getName());
		int i = 1;
		String tempId = newId;
		while (names.contains(tempId)) {
			tempId = newId + i;
			i++;
		}
		names.add(tempId);
		return tempId;
	}

	public void clear() {
		names.clear();
	}

	private Map<String, String> controllers = new HashMap<>();

	private Map<String, ClassOrInterfaceType> factories = new HashMap<>();

	public boolean containsFactory(Object key) {
		return factories.containsKey(key);
	}

	public ClassOrInterfaceType getFactory(Object key) {
		return factories.get(key);
	}

	public ClassOrInterfaceType putFactory(String key,
			ClassOrInterfaceType value) {
		return factories.put(key, value);
	}

	public String getController(String key) {
		return controllers.get(key);
	}

	public boolean containsController(String key) {
		return controllers.containsKey(key);
	}

	public String putController(String key, String value) {
		return controllers.put(key, value);
	}

	private String fxController = null;

	public String getFxController() {
		return fxController;
	}

	public void setFxController(String fxController) {
		this.fxController = fxController;
	}

}
