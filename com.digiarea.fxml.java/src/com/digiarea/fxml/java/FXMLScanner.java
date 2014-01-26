package com.digiarea.fxml.java;

import com.dagxp.core.utils.StringUtils;
import com.dagxp.lmm.jse.ClassOrInterfaceType;
import com.dagxp.lmm.jse.utils.NodeUtils;
import com.digiarea.fxml.Element;
import com.digiarea.fxml.Fxml;
import com.digiarea.fxml.IncludeElement;
import com.digiarea.fxml.InstanceDeclarationElement;
import com.digiarea.fxml.Node;
import com.digiarea.fxml.PropertyElement;
import com.digiarea.fxml.ValueElement;
import com.digiarea.fxml.visitor.VoidVisitorAdapter;

public class FXMLScanner extends VoidVisitorAdapter<Context> {

	@Override
	public void visit(Fxml n, Context ctx) throws Exception {
		if (n.getRoot() != null) {
			ValueElement root = (ValueElement) n.getRoot();
			String name = n.getName() + ".fxml";
			ctx.putController(name, root.getController());
			ClassOrInterfaceType type = NodeUtils.toClassOrInterfaceType(root
					.getName());
			ctx.putFactory(name, type);
		}
		super.visit(n, ctx);
	}

	@Override
	public void visit(InstanceDeclarationElement n, Context ctx)
			throws Exception {
		n.setIdentifier(getIdentifier(n.getFxId(), n.getId(), n.getName(), ctx));
		super.visit(n, ctx);
	}

	@Override
	public void visit(IncludeElement n, Context ctx) throws Exception {
		n.setIdentifier(getIdentifier(n.getFxId(), n.getId(), n.getName(), ctx));
		super.visit(n, ctx);
	}

	@Override
	public void visit(PropertyElement n, Context ctx) throws Exception {
		String propertyName = findPropertyName(n);
		n.setIdentifier(getIdentifier(null, n.getId(), propertyName, ctx));
		super.visit(n, ctx);
	}

	private String findPropertyName(PropertyElement n) {
		Node node = n;
		while (node != null && !(node instanceof ValueElement)) {
			node = node.getParent();
		}
		return StringUtils.firstToLower(((Element) node).getIdentifier())
				+ StringUtils.firstToUpper(n.getName());
	}

	private String getIdentifier(String fxId, String id, String name,
			Context ctx) {
		String identifier = fxId;
		if (identifier == null) {
			if (id != null) {
				identifier = ctx.getNewIdentifier(id);
			} else {
				identifier = ctx.getNewIdentifier(name);
			}
		}
		return identifier;

	}

}
