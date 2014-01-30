package com.digiarea.fxml.java;

public class FXMLUtils {

	public static com.digiarea.jse.Expression getEventHandler(String name) {
		return com.digiarea.jse.NodeFacade
				.ObjectCreationExpr(
						null,
						com.digiarea.jse.NodeFacade
								.ClassOrInterfaceType(
										null,
										com.digiarea.jse.NodeFacade
												.QualifiedNameExpr(
														com.digiarea.jse.NodeFacade
																.QualifiedNameExpr(
																		com.digiarea.jse.NodeFacade
																				.NameExpr("javafx"),
																		"event"),
														"EventHandler"),
										java.util.Arrays
												.asList((com.digiarea.jse.Type) com.digiarea.jse.NodeFacade
														.ReferenceType(
																com.digiarea.jse.NodeFacade
																		.ClassOrInterfaceType(
																				null,
																				com.digiarea.jse.NodeFacade
																						.QualifiedNameExpr(
																								com.digiarea.jse.NodeFacade
																										.QualifiedNameExpr(
																												com.digiarea.jse.NodeFacade
																														.NameExpr("javafx"),
																												"event"),
																								"ActionEvent"),
																				null),
																0))),
						null,
						null,
						java.util.Arrays
								.asList((com.digiarea.jse.BodyDeclaration) com.digiarea.jse.NodeFacade
										.MethodDeclaration(
												1,
												null,
												com.digiarea.jse.NodeFacade
														.VoidType(),
												"handle",
												java.util.Arrays
														.asList((com.digiarea.jse.Parameter) com.digiarea.jse.NodeFacade
																.Parameter(
																		0,
																		com.digiarea.jse.NodeFacade
																				.ReferenceType(
																						com.digiarea.jse.NodeFacade
																								.ClassOrInterfaceType(
																										null,
																										com.digiarea.jse.NodeFacade
																												.QualifiedNameExpr(
																														com.digiarea.jse.NodeFacade
																																.QualifiedNameExpr(
																																		com.digiarea.jse.NodeFacade
																																				.NameExpr("javafx"),
																																		"event"),
																														"ActionEvent"),
																										null),
																						0),
																		null,
																		com.digiarea.jse.NodeFacade
																				.VariableDeclaratorId(
																						"event",
																						null),
																		null)),
												null,
												null,
												com.digiarea.jse.NodeFacade
														.BlockStmt(java.util.Arrays
																.asList((com.digiarea.jse.Statement) com.digiarea.jse.NodeFacade
																		.ExpressionStmt(com.digiarea.jse.NodeFacade
																				.MethodCallExpr(
																						null,
																						null,
																						name,
																						java.util.Arrays
																								.asList((com.digiarea.jse.Expression) com.digiarea.jse.NodeFacade
																										.NameExpr("event")))))),
												null,
												java.util.Arrays
														.asList((com.digiarea.jse.AnnotationExpr) com.digiarea.jse.NodeFacade
																.MarkerAnnotationExpr(com.digiarea.jse.NodeFacade
																		.QualifiedNameExpr(
																				com.digiarea.jse.NodeFacade
																						.QualifiedNameExpr(
																								com.digiarea.jse.NodeFacade
																										.NameExpr("java"),
																								"lang"),
																				"Override"))))));
	}

}
