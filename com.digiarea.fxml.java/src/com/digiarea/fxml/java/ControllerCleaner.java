package com.digiarea.fxml.java;

import java.util.List;

import com.digiarea.jse.MethodDeclaration;
import com.digiarea.jse.Node;
import com.digiarea.jse.arrow.Context;
import com.digiarea.jse.arrow.Identity;

public class ControllerCleaner extends Identity {

	private List<String> names = null;

	public ControllerCleaner(List<String> names) {
		super();
		this.names = names;
	}

	@Override
	public Node visit(MethodDeclaration n, Context ctx) throws Exception {
		if (names.contains(n.getName())
				&& (n.getParameters() == null || n.getParameters().isEmpty())) {
			return null;
		} else {
			return super.visit(n, ctx);
		}
	}

}
