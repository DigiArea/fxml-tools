package com.digiarea.fxml.java;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dagxp.core.utils.StringUtils;
import com.dagxp.lmm.jse.AssignExpr;
import com.dagxp.lmm.jse.AssignExpr.AssignOperator;
import com.dagxp.lmm.jse.BlockStmt;
import com.dagxp.lmm.jse.CastExpr;
import com.dagxp.lmm.jse.ClassExpr;
import com.dagxp.lmm.jse.ClassOrInterfaceType;
import com.dagxp.lmm.jse.CompilationUnit;
import com.dagxp.lmm.jse.DoubleLiteralExpr;
import com.dagxp.lmm.jse.EnclosedExpr;
import com.dagxp.lmm.jse.Expression;
import com.dagxp.lmm.jse.ExpressionStmt;
import com.dagxp.lmm.jse.FieldAccessExpr;
import com.dagxp.lmm.jse.ImportDeclaration;
import com.dagxp.lmm.jse.MethodCallExpr;
import com.dagxp.lmm.jse.MethodDeclaration;
import com.dagxp.lmm.jse.ModifierSet;
import com.dagxp.lmm.jse.NameExpr;
import com.dagxp.lmm.jse.Node;
import com.dagxp.lmm.jse.NullLiteralExpr;
import com.dagxp.lmm.jse.ObjectCreationExpr;
import com.dagxp.lmm.jse.QualifiedNameExpr;
import com.dagxp.lmm.jse.ReturnStmt;
import com.dagxp.lmm.jse.Statement;
import com.dagxp.lmm.jse.StatementList;
import com.dagxp.lmm.jse.StringLiteralExpr;
import com.dagxp.lmm.jse.builder.ModelHierarchy;
import com.dagxp.lmm.jse.builder.ModelUpdater;
import com.dagxp.lmm.jse.utils.NodeUtils;
import com.digiarea.fxml.Attribute;
import com.digiarea.fxml.Comment;
import com.digiarea.fxml.CopyElement;
import com.digiarea.fxml.DefineElement;
import com.digiarea.fxml.Element;
import com.digiarea.fxml.Fxml;
import com.digiarea.fxml.ImportProcessing;
import com.digiarea.fxml.IncludeElement;
import com.digiarea.fxml.InstanceDeclarationElement;
import com.digiarea.fxml.LanguageProcessing;
import com.digiarea.fxml.ProcessingInstruction;
import com.digiarea.fxml.PropertyElement;
import com.digiarea.fxml.ReferenceElement;
import com.digiarea.fxml.RootElement;
import com.digiarea.fxml.ScriptElement;
import com.digiarea.fxml.UknownStaticPropertyElement;
import com.digiarea.fxml.UknownTypeElement;
import com.digiarea.fxml.ValueElement;
import com.digiarea.fxml.Attribute.AttributeType;
import com.digiarea.fxml.ImportProcessing.ImportType;
import com.digiarea.fxml.PropertyElement.PropertyType;
import com.digiarea.fxml.parser.Constants;
import com.digiarea.fxml.visitor.GenericVisitor;

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
				cu.setImports(imports);
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
				return new FieldAccessExpr(
						NodeUtils.createNameExpr(JAVAFX_CONTROL),
						"USE_PREF_SIZE");
			} else if (value.equals("-1.0")) {
				return new FieldAccessExpr(
						NodeUtils.createNameExpr(JAVAFX_CONTROL),
						"USE_COMPUTED_SIZE");
			} else {
				return new NameExpr(value);
			}
		} else if (name.equals(Constants.ID_ATTRIBUTE)) {
			return new StringLiteralExpr(value);
		} else if (value.startsWith("%")) {
			Expression arg = new StringLiteralExpr(value.substring(1));
			return new MethodCallExpr(new NameExpr(BUNDLE), GET_STRING,
					Arrays.asList(arg));
		} else if (value.startsWith("@")) {
			return new StringLiteralExpr(
					normilize(ctx.getFxController(), value));
		} else if (name.equals("text")) {
			return new StringLiteralExpr(value);
		} else if (isContentDisplay(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_CONTENT_DISPLAY), value);
		} else if (isPriority(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_SCENE_LAYOUT_PRIORITY),
					value);
		} else if (isTabClosingPolicy(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_TAB_CLOSING_POLICY), value);
		} else if (isTextAlignment(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_TEXT_ALIGNMENT), value);
		} else if (isVPos(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_GEOMETRY_V_POS), value);
		} else if (isHPos(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_GEOMETRY_H_POS), value);
		} else if (isPosition(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_GEOMETRY_POS), value);
		} else if (isOverrunStyle(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_OVERRUN_STYLE), value);
		} else if (isSide(name, value)) {
			return new FieldAccessExpr(
					NodeUtils.createNameExpr(JAVAFX_GEOMETRY_SIDE), value);
		} else {
			if (value.startsWith("$")) {
				return new NameExpr(value.substring(1));
			}
			return new NameExpr(value);
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
		Expression scope = new NameExpr(parentIdentifier);
		List<Expression> args = new ArrayList<>();
		AttributeType attributeType = n.getAttributeType();
		String localName = n.getName();
		String methodName = getSetterName(localName);
		if (localName.equals(STYLE_CLASS)) {
			scope = new MethodCallExpr(scope, "getStyleClass");
			args.add(new StringLiteralExpr(n.getValue()));
			methodName = ADD;
		} else if (localName.equals("url")) {
			scope = new MethodCallExpr(null, GET_CLASS);
			Expression arg = resolve(n, ctx);
			scope = new MethodCallExpr(scope, GET_RESOURCE, Arrays.asList(arg));
			return new MethodCallExpr(scope, OPEN_STREAM);
		} else if (attributeType == AttributeType.EVENT_HANDLER) {
			args.add(FXMLUtils.getEventHandler(n.getValue().substring(1)));
		} else if (attributeType == AttributeType.INSTANCE_PROPERTY) {
			args.add(resolve(n, ctx));
		} else if (attributeType == AttributeType.STATIC_PROPERTY) {
			args.add(scope);
			args.add(resolve(n, ctx));
			QualifiedNameExpr qName = (QualifiedNameExpr) NodeUtils
					.createNameExpr(localName);
			scope = qName.getQualifier();
			methodName = getSetterName(qName.getName());
		}
		return new ExpressionStmt(new MethodCallExpr(scope, methodName, args));
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
			ClassOrInterfaceType type = NodeUtils.toClassOrInterfaceType(root
					.getName());
			if (fxController != null) {
				ctx.setFxController(fxController);
				// process the root
				StatementList list = (StatementList) root.accept(this, ctx);
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
					MethodDeclaration method = new MethodDeclaration(
							ModifierSet.PUBLIC, type, METHOD_NAME);
					method.setBody(new BlockStmt(list.getStatements()));
					method.setThrowsList(Arrays.asList(NodeUtils
							.createNameExpr("java.lang.Exception")));
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
			return new ImportDeclaration(
					NodeUtils.createNameExpr(n.getValue()), false,
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
		ClassOrInterfaceType type = NodeUtils
				.toClassOrInterfaceType(controller);
		Expression arg = new ClassExpr(type);
		arg = new MethodCallExpr(NodeUtils.getGetterCall(new NameExpr(
				MODEL_FACADE), FACTORY, false), CALL, Arrays.asList(arg));
		arg = new EnclosedExpr(new CastExpr(type, arg));
		arg = new MethodCallExpr(arg, METHOD_NAME);
		statements.add(new ExpressionStmt(NodeUtils
				.createVariableDeclarationExpr(factoryType, n.getIdentifier(),
						arg)));
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
		return new StatementList(statements, null);
	}

	@Override
	public Node visit(InstanceDeclarationElement n, Context ctx)
			throws Exception {
		List<Statement> statements = new ArrayList<>();
		String name = n.getName();
		ClassOrInterfaceType type = NodeUtils.toClassOrInterfaceType(name);
		List<Element> elements = n.getElements();
		boolean hasElements = elements != null && elements.size() > 0;
		// TODO initialize as a value, constant or factory :)
		List<Expression> arguments = new ArrayList<>();
		Expression init = new ObjectCreationExpr(type, arguments);
		String identifier = n.getIdentifier();
		if (identifier.equals(n.getFxId())) {
			statements.add(new ExpressionStmt(new AssignExpr(new NameExpr(
					identifier), init, AssignOperator.assign)));
		} else {
			statements.add(new ExpressionStmt(NodeUtils
					.createVariableDeclarationExpr(type, identifier, init)));
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
					StatementList list = (StatementList) item.accept(this, ctx);
					if (list != null) {
						statements.addAll(list.getStatements());
					}
				}
			}
		}
		if (isRoot(n)) {
			List<Expression> args = new ArrayList<>();
			args.add(new NullLiteralExpr());
			args.add(new NameExpr(BUNDLE));
			statements.add(new ExpressionStmt(new MethodCallExpr(null,
					INITIALIZE, args)));
			statements.add(new ReturnStmt(new NameExpr(identifier)));
		}
		return new StatementList(statements, null);
	}

	private void makeInsets(List<Attribute> attributes,
			List<Expression> arguments) {
		// Insets(double top, double right, double bottom, double left)
		arguments.add(new DoubleLiteralExpr(selectAttribute(attributes, "top",
				"0.0")));
		arguments.add(new DoubleLiteralExpr(selectAttribute(attributes,
				"right", "0.0")));
		arguments.add(new DoubleLiteralExpr(selectAttribute(attributes,
				"bottom", "0.0")));
		arguments.add(new DoubleLiteralExpr(selectAttribute(attributes, "left",
				"0.0")));

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
	public Node visit(com.digiarea.fxml.Project n, Context ctx) throws Exception {
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
					StatementList list = (StatementList) item.accept(this, ctx);
					if (list != null) {
						Expression arg = new NameExpr(item.getIdentifier());
						statements.addAll(list.getStatements());
						if (isList) {
							statements.add(new ExpressionStmt(
									new MethodCallExpr(NodeUtils.getGetterCall(
											new NameExpr(parentIdentifier),
											name, false), ADD, Arrays
											.asList(arg))));
						} else {
							if (n.getPropertyType() == PropertyType.STATIC_PROPERTY) {
								List<Expression> args = new ArrayList<>();
								args.add(new NameExpr(parentIdentifier));
								args.add(arg);
								QualifiedNameExpr qName = (QualifiedNameExpr) NodeUtils
										.createNameExpr(name);
								Expression scope = qName.getQualifier();
								String methodName = getSetterName(qName
										.getName());
								statements.add(new ExpressionStmt(
										new MethodCallExpr(scope, methodName,
												args)));
							} else {
								statements.add(new ExpressionStmt(NodeUtils
										.getSetterCall(new NameExpr(
												parentIdentifier), n.getName(),
												arg, false)));
							}
						}
					}
				}
			}
		}
		return new StatementList(statements, null);
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
