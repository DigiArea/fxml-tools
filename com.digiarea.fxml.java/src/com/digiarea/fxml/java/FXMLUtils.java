package com.digiarea.fxml.java;

import com.dagxp.lmm.jse.Expression;

public class FXMLUtils {

	public static Expression getEventHandler(String name) {
		return new com.dagxp.lmm.jse.ObjectCreationExpr(
				null,
				new com.dagxp.lmm.jse.ClassOrInterfaceType(
						null,
						new com.dagxp.lmm.jse.QualifiedNameExpr(
								new com.dagxp.lmm.jse.QualifiedNameExpr(
										new com.dagxp.lmm.jse.NameExpr("javafx"),
										"event"), "EventHandler"),
						java.util.Arrays
								.asList((com.dagxp.lmm.jse.Type) new com.dagxp.lmm.jse.ReferenceType(
										new com.dagxp.lmm.jse.ClassOrInterfaceType(
												null,
												new com.dagxp.lmm.jse.QualifiedNameExpr(
														new com.dagxp.lmm.jse.QualifiedNameExpr(
																new com.dagxp.lmm.jse.NameExpr(
																		"javafx"),
																"event"),
														"ActionEvent"), null),
										0))),
				null,
				null,
				java.util.Arrays
						.asList((com.dagxp.lmm.jse.BodyDeclaration) new com.dagxp.lmm.jse.MethodDeclaration(
								null,
								1,
								java.util.Arrays
										.asList((com.dagxp.lmm.jse.AnnotationExpr) new com.dagxp.lmm.jse.MarkerAnnotationExpr(
												new com.dagxp.lmm.jse.QualifiedNameExpr(
														new com.dagxp.lmm.jse.QualifiedNameExpr(
																new com.dagxp.lmm.jse.NameExpr(
																		"java"),
																"lang"),
														"Override"))),
								null,
								new com.dagxp.lmm.jse.VoidType(),
								"handle",
								java.util.Arrays
										.asList((com.dagxp.lmm.jse.Parameter) new com.dagxp.lmm.jse.Parameter(
												0,
												null,
												new com.dagxp.lmm.jse.ReferenceType(
														new com.dagxp.lmm.jse.ClassOrInterfaceType(
																null,
																new com.dagxp.lmm.jse.QualifiedNameExpr(
																		new com.dagxp.lmm.jse.QualifiedNameExpr(
																				new com.dagxp.lmm.jse.NameExpr(
																						"javafx"),
																				"event"),
																		"ActionEvent"),
																null), 0),
												false,
												new com.dagxp.lmm.jse.VariableDeclaratorId(
														"event", 0))),
								0,
								null,
								new com.dagxp.lmm.jse.BlockStmt(
										java.util.Arrays
												.asList((com.dagxp.lmm.jse.Statement) new com.dagxp.lmm.jse.ExpressionStmt(
														new com.dagxp.lmm.jse.MethodCallExpr(
																null,
																null,
																name,
																java.util.Arrays
																		.asList((com.dagxp.lmm.jse.Expression) new com.dagxp.lmm.jse.NameExpr(
																				"event")))))))));
	}

}
