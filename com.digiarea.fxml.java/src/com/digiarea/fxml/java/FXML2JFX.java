package com.digiarea.fxml.java;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.digiarea.common.utils.StringUtils;
import com.digiarea.fxml.Attribute;
import com.digiarea.fxml.Attribute.AttributeType;
import com.digiarea.fxml.Comment;
import com.digiarea.fxml.CopyElement;
import com.digiarea.fxml.DefineElement;
import com.digiarea.fxml.Element;
import com.digiarea.fxml.Fxml;
import com.digiarea.fxml.ImportProcessing;
import com.digiarea.fxml.ImportProcessing.ImportType;
import com.digiarea.fxml.IncludeElement;
import com.digiarea.fxml.InstanceDeclarationElement;
import com.digiarea.fxml.LanguageProcessing;
import com.digiarea.fxml.ProcessingInstruction;
import com.digiarea.fxml.PropertyElement;
import com.digiarea.fxml.PropertyElement.PropertyType;
import com.digiarea.fxml.ReferenceElement;
import com.digiarea.fxml.RootElement;
import com.digiarea.fxml.ScriptElement;
import com.digiarea.fxml.UknownStaticPropertyElement;
import com.digiarea.fxml.UknownTypeElement;
import com.digiarea.fxml.ValueElement;
import com.digiarea.fxml.parser.Constants;
import com.digiarea.fxml.visitor.GenericVisitor;
import com.digiarea.jse.AssignExpr.AssignOperator;
import com.digiarea.jse.ClassOrInterfaceType;
import com.digiarea.jse.CompilationUnit;
import com.digiarea.jse.Expression;
import com.digiarea.jse.ImportDeclaration;
import com.digiarea.jse.MethodDeclaration;
import com.digiarea.jse.Modifiers;
import com.digiarea.jse.Node;
import com.digiarea.jse.NodeFacade;
import com.digiarea.jse.NodeList;
import com.digiarea.jse.QualifiedNameExpr;
import com.digiarea.jse.Statement;
import com.digiarea.jse.builder.ModelHierarchy;
import com.digiarea.jse.builder.ModelUpdater;
import com.digiarea.jse.utils.NodeUtils;

public class FXML2JFX implements GenericVisitor<Node, Context> {

	private static final String JAVAFX_CONTROL = "javafx.scene.control.Control";
	private static final String INITIALIZE = "initialize";
	private static final String JAVAFX_GEOMETRY_SIDE = "javafx.geometry.Side";
	private static final String JAVAFX_OVERRUN_STYLE = "javafx.scene.control.OverrunStyle";
	private static final String JAVAFX_TEXT_ALIGNMENT = "javafx.scene.text.TextAlignment";
	private static final String JAVAFX_GEOMETRY_H_POS = "javafx.geometry.HPos";
	private static final String JAVAFX_GEOMETRY_V_POS = "javafx.geometry.VPos";
	private static final String JAVAFX_TAB_CLOSING_POLICY = "javafx.scene.control.TabPane.TabClosingPolicy";
	private static final String JAVAFX_CONTENT_DISPLAY = "javafx.scene.control.ContentDisplay";
	private static final String JAVAFX_SCENE_LAYOUT_PRIORITY = "javafx.scene.layout.Priority";
	private static final String JAVAFX_GEOMETRY_POS = "javafx.geometry.Pos";
	private static final String STYLE_CLASS = "styleClass";
	private static final String OPEN_STREAM = "openStream";
	private static final String GET_RESOURCE = "getResource";
	private static final String GET_CLASS = "getClass";
	private static final String GET_STRING = "getString";
	private static final String BUNDLE = "bundle";
	private static final String MODEL_FACADE = "modelFacade";
	private static final String FACTORY = "factory";
	private static final String CALL = "call";
	private static final String ADD = "add";
	private static final String JAVA_LANG = "java.lang";
	private static final String METHOD_NAME = "create";

	private static final Map<String, String> listTypes = new HashMap<>();

	static {
		listTypes.put("children", "javafx.scene.Node");
		listTypes.put("columnConstraints",
				"javafx.scene.layout.ColumnConstraints");
		listTypes.put("rowConstraints", "javafx.scene.layout.RowConstraints");
		listTypes.put("tabs", "javafx.scene.control.Tab");
		listTypes.put("items", "javafx.scene.control.MenuItem");
		listTypes.put("columns", "javafx.scene.control.TableColumn");
	}

	private List<ImportDeclaration> getImports(ModelHierarchy hierarchy,
			String fxController) throws Exception {
		CompilationUnit cu = NodeUtils.selectCompilationUnit(
				hierarchy.getProject(), fxController);
		if (cu != null) {
			List<ImportDeclaration> imports = cu.getImports();
			if (imports == null) {
				imports = new ArrayList<>();
				cu.setImports(NodeFacade.NodeList(imports));
			}
			return imports;
		} else {
			throw new Exception("Can not find compilation unit for type: "
					+ fxController);
		}
	}

	private boolean isRoot(Element n) {
		return n.getParent() != null && n.getParent() instanceof Fxml;
	}

	private String getSetterName(String localName) {
		return "set" + StringUtils.firstToUpper(localName);
	}

	private Expression resolve(Attribute n, Context ctx) {
		String name = n.getName();
		String value = n.getValue();
		if (name.startsWith("max") || name.startsWith("min")
				|| name.startsWith("pref")) {
			if (value.equals("-Infinity")) {
				return NodeFacade.FieldAccessExpr(JAVAFX_CONTROL,
						"USE_PREF_SIZE");
			} else if (value.equals("-1.0")) {
				return NodeFacade.FieldAccessExpr(JAVAFX_CONTROL,
						"USE_COMPUTED_SIZE");
			} else {
				return NodeFacade.NameExpr(value);
			}
		} else if (name.equals(Constants.ID_ATTRIBUTE)) {
			return NodeFacade.StringLiteralExpr(value);
		} else if (value.startsWith("%")) {
			Expression arg = NodeFacade.StringLiteralExpr(value.substring(1));
			return NodeFacade.MethodCallExpr(NodeFacade.NameExpr(BUNDLE), null,
					GET_STRING, Arrays.asList(arg));
		} else if (value.startsWith("@")) {
			return NodeFacade.StringLiteralExpr(normilize(
					ctx.getFxController(), value));
		} else if (name.equals("text")) {
			return NodeFacade.StringLiteralExpr(value);
		} else if (isContentDisplay(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_CONTENT_DISPLAY, value);
		} else if (isPriority(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_SCENE_LAYOUT_PRIORITY,
					value);
		} else if (isTabClosingPolicy(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_TAB_CLOSING_POLICY, value);
		} else if (isTextAlignment(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_TEXT_ALIGNMENT, value);
		} else if (isVPos(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_GEOMETRY_V_POS, value);
		} else if (isHPos(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_GEOMETRY_H_POS, value);
		} else if (isPosition(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_GEOMETRY_POS, value);
		} else if (isOverrunStyle(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_OVERRUN_STYLE, value);
		} else if (isSide(name, value)) {
			return NodeFacade.FieldAccessExpr(JAVAFX_GEOMETRY_SIDE, value);
		} else {
			if (value.startsWith("$")) {
				return NodeFacade.NameExpr(value.substring(1));
			}
			return NodeFacade.NameExpr(value);
		}
	}

	private boolean isTextAlignment(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("textAlignment");
	}

	private boolean isSide(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("side");
	}

	private boolean isHPos(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("halignment");
	}

	private boolean isVPos(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("valignment");
	}

	private boolean isTabClosingPolicy(String name, String value) {
		Constants.TabClosingPolicy[] values = Constants.TabClosingPolicy
				.values();
		for (int i = 0; i < values.length; i++) {
			if (value.equals(values[i].name())) {
				return true;
			}
		}
		return false;
	}

	private boolean isOverrunStyle(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("textOverrun");
	}

	private boolean isContentDisplay(String name, String value) {
		String local = name;
		int index = local.lastIndexOf(".");
		if (index != -1) {
			local = local.substring(index + 1);
		}
		return local.equals("contentDisplay");
	}

	private boolean isPriority(String name, String value) {
		Constants.Priority[] priorities = Constants.Priority.values();
		for (int i = 0; i < priorities.length; i++) {
			if (value.equals(priorities[i].name())) {
				return true;
			}
		}
		return false;
	}

	private boolean isPosition(String name, String value) {
		Constants.Pos[] positions = Constants.Pos.values();
		for (int i = 0; i < positions.length; i++) {
			if (value.equals(positions[i].name())) {
				return true;
			}
		}
		return false;
	}

	private String normilize(String controller, String value) {
		Path path = Paths.get(value.substring(1));
		Path real = Paths
				.get(NodeUtils.createPathFromQualifiedName(controller));
		real = real.getParent();
		while (path.startsWith("../")) {
			path = path.subpath(1, path.getNameCount());
			real = real.getParent();
		}
		return "/" + real.resolve(path).toString().replaceAll("\\\\", "/");
	}

	@Override
	public Node visit(Attribute n, Context ctx) throws Exception {
		String parentIdentifier = ((Element) n.getParent()).getIdentifier();
		Expression scope = NodeFacade.NameExpr(parentIdentifier);
		List<Expression> args = new ArrayList<>();
		AttributeType attributeType = n.getAttributeType();
		String localName = n.getName();
		String methodName = getSetterName(localName);
		if (localName.equals(STYLE_CLASS)) {
			scope = NodeFacade.MethodCallExpr(scope, "getStyleClass");
			args.add(NodeFacade.StringLiteralExpr(n.getValue()));
			methodName = ADD;
		} else if (localName.equals("url")) {
			scope = NodeFacade.MethodCallExpr(null, GET_CLASS);
			Expression arg = resolve(n, ctx);
			scope = NodeFacade.MethodCallExpr(scope, null, GET_RESOURCE,
					Arrays.asList(arg));
			return NodeFacade.MethodCallExpr(scope, OPEN_STREAM);
		} else if (attributeType == AttributeType.EVENT_HANDLER) {
			args.add(FXMLUtils.getEventHandler(n.getValue().substring(1)));
		} else if (attributeType == AttributeType.INSTANCE_PROPERTY) {
			args.add(resolve(n, ctx));
		} else if (attributeType == AttributeType.STATIC_PROPERTY) {
			args.add(scope);
			args.add(resolve(n, ctx));
			QualifiedNameExpr qName = (QualifiedNameExpr) NodeFacade
					.QualifiedNameExpr(localName);
			scope = qName.getQualifier();
			methodName = getSetterName(qName.getName());
		}
		return NodeFacade.ExpressionStmt(NodeFacade.MethodCallExpr(scope, null,
				methodName, args));
	}

	@Override
	public Node visit(AttributeType n, Context ctx) throws Exception {
		// nothing to do
		return null;
	}

	@Override
	public Node visit(Comment n, Context ctx) throws Exception {
		// nothing to do
		return null;
	}

	@Override
	public Node visit(CopyElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(DefineElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(Fxml n, Context ctx) throws Exception {
		if (n.getRoot() != null && (n.getRoot() instanceof ValueElement)) {
			ctx.clear();
			ValueElement root = (ValueElement) n.getRoot();
			// controller
			String fxController = root.getController();
			ClassOrInterfaceType type = NodeFacade.ClassOrInterfaceType(root
					.getName());
			if (fxController != null) {
				ctx.setFxController(fxController);
				// process the root
				NodeList<Statement> list = (NodeList<Statement>) root.accept(
						this, ctx);
				ModelUpdater updater = hierarchy.getUpdater(fxController);
				if (updater != null) {
					// process instructions
					if (n.getProcessingInstructions() != null) {
						List<ImportDeclaration> imports = getImports(hierarchy,
								fxController);
						for (ProcessingInstruction item : n
								.getProcessingInstructions()) {
							if (item != null) {
								Node node = item.accept(this, ctx);
								if (node != null
										&& node instanceof ImportDeclaration) {
									imports.add((ImportDeclaration) node);
								}
							}
						}
					}
					// make method
					MethodDeclaration method = NodeFacade.MethodDeclaration(
							Modifiers.PUBLIC, type, METHOD_NAME);
					method.setBlock(NodeFacade.BlockStmt(list.getNodes()));
					method.setThrowsList(NodeFacade.NodeList(Arrays.asList(NodeFacade
							.ClassOrInterfaceType("java.lang.Exception"))));
					updater.addMember(method);
					ctx.setFxController(null);
					return method;
				} else {
					throw new Exception("No updater found for qualified name: "
							+ fxController);
				}
			} else {
				throw new Exception("No fx:controller attribute in "
						+ n.getName());
			}
		} else {
			throw new Exception("No root element found in " + n.getName());
		}
	}

	@Override
	public Node visit(ImportProcessing n, Context ctx) throws Exception {
		if (!JAVA_LANG.equals(n.getValue())) {
			return NodeFacade.ImportDeclaration(
					NodeFacade.QualifiedNameExpr(n.getValue()), false,
					n.getImportType() == ImportType.IMPORT_PACKAGE);
		}
		return null;
	}

	@Override
	public Node visit(ImportType n, Context ctx) throws Exception {
		// nothing to do
		return null;
	}

	@Override
	public Node visit(IncludeElement n, Context ctx) throws Exception {
		List<Statement> statements = new ArrayList<>();
		String source = n.getSource();
		String controller = ctx.getController(source);
		ClassOrInterfaceType factoryType = ctx.getFactory(source);
		ClassOrInterfaceType type = NodeFacade.ClassOrInterfaceType(controller);
		Expression arg = NodeFacade.ClassExpr(type);
		arg = NodeFacade.MethodCallExpr(NodeUtils.getGetterCall(
				NodeFacade.NameExpr(MODEL_FACADE), FACTORY, false), null, CALL,
				Arrays.asList(arg));
		arg = NodeFacade.EnclosedExpr(NodeFacade.CastExpr(type, arg));
		arg = NodeFacade.MethodCallExpr(arg, METHOD_NAME);
		statements.add(NodeFacade.ExpressionStmt(NodeFacade
				.VariableDeclarationExpr(factoryType, n.getIdentifier(), arg)));
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					Node attribute = item.accept(this, ctx);
					if (attribute != null) {
						statements.add((Statement) attribute);
					}
				}
			}
		}
		return NodeFacade.NodeList(statements);
	}

	@Override
	public Node visit(InstanceDeclarationElement n, Context ctx)
			throws Exception {
		List<Statement> statements = new ArrayList<>();
		String name = n.getName();
		ClassOrInterfaceType type = NodeFacade.ClassOrInterfaceType(name);
		List<Element> elements = n.getElements();
		boolean hasElements = elements != null && elements.size() > 0;
		// TODO initialize as a value, constant or factory :)
		List<Expression> arguments = new ArrayList<>();
		Expression init = NodeFacade.ObjectCreationExpr(type, arguments);
		String identifier = n.getIdentifier();
		if (identifier.equals(n.getFxId())) {
			statements.add(NodeFacade.ExpressionStmt(NodeFacade.AssignExpr(
					NodeFacade.NameExpr(identifier), init,
					AssignOperator.assign)));
		} else {
			statements.add(NodeFacade.ExpressionStmt(NodeFacade
					.VariableDeclarationExpr(type, identifier, init)));
		}
		if (n.getAttributes() != null) {
			if (name.equals("Insets")) {
				makeInsets(n.getAttributes(), arguments);
			} else {
				for (Attribute item : n.getAttributes()) {
					if (item != null) {
						Node attribute = item.accept(this, ctx);
						if (attribute != null) {
							if (name.equals("Image")) {
								arguments.add((Expression) attribute);
							} else {

								statements.add((Statement) attribute);
							}
						}
					}
				}
			}
		}
		if (hasElements) {
			for (Element item : elements) {
				if (item != null) {
					NodeList<Statement> list = (NodeList<Statement>) item
							.accept(this, ctx);
					if (list != null) {
						statements.addAll(list.getNodes());
					}
				}
			}
		}
		if (isRoot(n)) {
			List<Expression> args = new ArrayList<>();
			args.add(NodeFacade.NullLiteralExpr());
			args.add(NodeFacade.NameExpr(BUNDLE));
			statements.add(NodeFacade.ExpressionStmt(NodeFacade.MethodCallExpr(
					INITIALIZE, args)));
			statements.add(NodeFacade.ReturnStmt(NodeFacade
					.NameExpr(identifier)));
		}
		return NodeFacade.NodeList(statements);
	}

	private void makeInsets(List<Attribute> attributes,
			List<Expression> arguments) {
		// Insets(double top, double right, double bottom, double left)
		arguments.add(NodeFacade.DoubleLiteralExpr(selectAttribute(attributes,
				"top", "0.0")));
		arguments.add(NodeFacade.DoubleLiteralExpr(selectAttribute(attributes,
				"right", "0.0")));
		arguments.add(NodeFacade.DoubleLiteralExpr(selectAttribute(attributes,
				"bottom", "0.0")));
		arguments.add(NodeFacade.DoubleLiteralExpr(selectAttribute(attributes,
				"left", "0.0")));

	}

	private String selectAttribute(List<Attribute> attributes, String name,
			String value) {
		for (Attribute attribute : attributes) {
			if (attribute.getName().equals(name)) {
				return attribute.getValue();
			}
		}
		return value;
	}

	@Override
	public Node visit(LanguageProcessing n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(com.digiarea.fxml.Project n, Context ctx)
			throws Exception {
		if (n.getFxmls() != null) {
			for (Fxml item : n.getFxmls()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(PropertyElement n, Context ctx) throws Exception {
		Element parent = (Element) n.getParent();
		List<Statement> statements = new ArrayList<>();
		List<Element> elements = n.getElements();
		String parentIdentifier = parent.getIdentifier();
		if (elements != null && elements.size() > 0) {
			String name = n.getName();
			boolean isList = listTypes.containsKey(name);
			for (Element item : elements) {
				if (item != null) {
					NodeList<Statement> list = (NodeList<Statement>) item
							.accept(this, ctx);
					if (list != null) {
						Expression arg = NodeFacade.NameExpr(item
								.getIdentifier());
						statements.addAll(list.getNodes());
						if (isList) {
							statements
									.add(NodeFacade.ExpressionStmt(NodeFacade.MethodCallExpr(
											NodeUtils.getGetterCall(
													NodeFacade
															.NameExpr(parentIdentifier),
													name, false), null, ADD,
											Arrays.asList(arg))));
						} else {
							if (n.getPropertyType() == PropertyType.STATIC_PROPERTY) {
								List<Expression> args = new ArrayList<>();
								args.add(NodeFacade.NameExpr(parentIdentifier));
								args.add(arg);
								QualifiedNameExpr qName = (QualifiedNameExpr) NodeFacade
										.QualifiedNameExpr(name);
								Expression scope = qName.getQualifier();
								String methodName = getSetterName(qName
										.getName());
								statements.add(NodeFacade
										.ExpressionStmt(NodeFacade
												.MethodCallExpr(scope, null,
														methodName, args)));
							} else {
								statements
										.add(NodeFacade.ExpressionStmt(NodeUtils.getSetterCall(
												NodeFacade
														.NameExpr(parentIdentifier),
												n.getName(), arg, false)));
							}
						}
					}
				}
			}
		}
		return NodeFacade.NodeList(statements);
	}

	@Override
	public Node visit(PropertyType n, Context ctx) throws Exception {
		// nothing to do
		return null;
	}

	@Override
	public Node visit(ReferenceElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(RootElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(ScriptElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(UknownStaticPropertyElement n, Context ctx)
			throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	@Override
	public Node visit(UknownTypeElement n, Context ctx) throws Exception {
		// TODO Auto-generated method stub
		if (n.getAttributes() != null) {
			for (Attribute item : n.getAttributes()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		if (n.getElements() != null) {
			for (Element item : n.getElements()) {
				if (item != null) {
					item.accept(this, ctx);
				}
			}
		}
		return null;
	}

	private ModelHierarchy hierarchy = null;

	public FXML2JFX(ModelHierarchy hierarchy) {
		super();
		this.hierarchy = hierarchy;
	}

}
