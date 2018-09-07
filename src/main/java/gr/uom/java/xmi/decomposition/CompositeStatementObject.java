package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;

public class CompositeStatementObject extends AbstractStatement {

	private List<AbstractStatement> statementList;
	private List<AbstractExpression> expressionList;
	private List<VariableDeclaration> variableDeclarations;
	private String type;
	private LocationInfo locationInfo;

	public CompositeStatementObject(CompilationUnit cu, String filePath, Statement statement, int depth, String type) {
		super();
		this.type = type;
		this.setDepth(depth);
		this.locationInfo = new LocationInfo(cu, filePath, statement);
		this.statementList = new ArrayList<AbstractStatement>();
		this.expressionList = new ArrayList<AbstractExpression>();
		this.variableDeclarations = new ArrayList<VariableDeclaration>();
	}

	public void addStatement(AbstractStatement statement) {
		statement.setIndex(statementList.size());
		statementList.add(statement);
		statement.setParent(this);
	}

	public List<AbstractStatement> getStatements() {
		return statementList;
	}

	public void addExpression(AbstractExpression expression) {
		//an expression has the same index and depth as the composite statement it belong to
		expression.setDepth(this.getDepth());
		expression.setIndex(this.getIndex());
		expressionList.add(expression);
		expression.setOwner(this);
	}

	public List<AbstractExpression> getExpressions() {
		return expressionList;
	}

	public void addVariableDeclaration(VariableDeclaration declaration) {
		this.variableDeclarations.add(declaration);
	}

	@Override
	public List<StatementObject> getLeaves() {
		List<StatementObject> leaves = new ArrayList<StatementObject>();
		for(AbstractStatement statement : statementList) {
			leaves.addAll(statement.getLeaves());
		}
		return leaves;
	}

	public List<CompositeStatementObject> getInnerNodes() {
		List<CompositeStatementObject> innerNodes = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				innerNodes.addAll(composite.getInnerNodes());
			}
		}
		innerNodes.add(this);
		return innerNodes;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		if(expressionList.size() > 0) {
			sb.append("(");
			for(int i=0; i<expressionList.size()-1; i++) {
				sb.append(expressionList.get(i).toString()).append("; ");
			}
			sb.append(expressionList.get(expressionList.size()-1).toString());
			sb.append(")");
		}
		return sb.toString();
	}

	@Override
	public List<String> getVariables() {
		List<String> variables = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			variables.addAll(expression.getVariables());
		}
		return variables;
	}

	@Override
	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			types.addAll(expression.getTypes());
		}
		return types;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		//special handling for enhanced-for formal parameter
		variableDeclarations.addAll(this.variableDeclarations);
		for(AbstractExpression expression : expressionList) {
			variableDeclarations.addAll(expression.getVariableDeclarations());
		}
		return variableDeclarations;
	}

	@Override
	public Map<String, OperationInvocation> getMethodInvocationMap() {
		Map<String, OperationInvocation> map = new LinkedHashMap<String, OperationInvocation>();
		for(AbstractExpression expression : expressionList) {
			map.putAll(expression.getMethodInvocationMap());
		}
		return map;
	}

	@Override
	public List<String> getAnonymousClassDeclarations() {
		List<String> anonymousClassDeclarations = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			anonymousClassDeclarations.addAll(expression.getAnonymousClassDeclarations());
		}
		return anonymousClassDeclarations;
	}

	@Override
	public List<String> getStringLiterals() {
		List<String> stringLiterals = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			stringLiterals.addAll(expression.getStringLiterals());
		}
		return stringLiterals;
	}

	@Override
	public List<String> getInfixOperators() {
		List<String> infixOperators = new ArrayList<String>();
		for(AbstractExpression expression : expressionList) {
			infixOperators.addAll(expression.getInfixOperators());
		}
		return infixOperators;
	}

	@Override
	public Map<String, ObjectCreation> getCreationMap() {
		Map<String, ObjectCreation> creationMap = new LinkedHashMap<String, ObjectCreation>();
		for(AbstractExpression expression : expressionList) {
			creationMap.putAll(expression.getCreationMap());
		}
		return creationMap;
	}

	public Map<String, OperationInvocation> getAllMethodInvocations() {
		Map<String, OperationInvocation> map = new LinkedHashMap<String, OperationInvocation>();
		map.putAll(getMethodInvocationMap());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				map.putAll(composite.getAllMethodInvocations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				map.putAll(statementObject.getMethodInvocationMap());
			}
		}
		return map;
	}

	public List<String> getAllAnonymousClassDeclarations() {
		List<String> anonymousClassDeclarations = new ArrayList<String>();
		anonymousClassDeclarations.addAll(getAnonymousClassDeclarations());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				anonymousClassDeclarations.addAll(composite.getAllAnonymousClassDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				anonymousClassDeclarations.addAll(statementObject.getAnonymousClassDeclarations());
			}
		}
		return anonymousClassDeclarations;
	}

	public List<String> getAllVariables() {
		List<String> variables = new ArrayList<String>();
		variables.addAll(getVariables());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variables.addAll(composite.getAllVariables());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				variables.addAll(statementObject.getVariables());
			}
		}
		return variables;
	}

	public List<VariableDeclaration> getAllVariableDeclarations() {
		List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
		variableDeclarations.addAll(getVariableDeclarations());
		for(AbstractStatement statement : statementList) {
			if(statement instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement;
				variableDeclarations.addAll(composite.getAllVariableDeclarations());
			}
			else if(statement instanceof StatementObject) {
				StatementObject statementObject = (StatementObject)statement;
				variableDeclarations.addAll(statementObject.getVariableDeclarations());
			}
		}
		return variableDeclarations;
	}

	@Override
	public int statementCount() {
		int count = 0;
		if(!this.getString().equals("{"))
			count++;
		for(AbstractStatement statement : statementList) {
			count += statement.statementCount();
		}
		return count;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getAllVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		return null;
	}

	protected double compositeChildMatchingScore(CompositeStatementObject other, List<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		int childrenSize1 = getStatements().size();
		int childrenSize2 = other.getStatements().size();
		
		int mappedChildrenSize = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(getStatements().contains(mapping.getFragment1()) && other.getStatements().contains(mapping.getFragment2())) {
				mappedChildrenSize++;
			}
		}
		if(mappedChildrenSize == 0) {
			List<StatementObject> leaves1 = getLeaves();
			List<StatementObject> leaves2 = other.getLeaves();
			int leaveSize1 = leaves1.size();
			int leaveSize2 = leaves2.size();
			int mappedLeavesSize = 0;
			for(AbstractCodeMapping mapping : mappings) {
				if(leaves1.contains(mapping.getFragment1()) && leaves2.contains(mapping.getFragment2())) {
					mappedLeavesSize++;
				}
			}
			if(mappedLeavesSize == 0) {
				//check for possible extract or inline
				if(leaveSize2 == 1) {
					OperationInvocation invocation = leaves2.get(0).invocationCoveringEntireFragment();
					if(invocation != null && matchesOperation(invocation, addedOperations)) {
						mappedLeavesSize++;
					}
				}
				else if(leaveSize1 == 1) {
					OperationInvocation invocation = leaves1.get(0).invocationCoveringEntireFragment();
					if(invocation != null && matchesOperation(invocation, removedOperations)) {
						mappedLeavesSize++;
					}
				}
			}
			int max = Math.max(leaveSize1, leaveSize2);
			if(max == 0)
				return 0;
			else
				return (double)mappedLeavesSize/(double)max;
		}
		
		int max = Math.max(childrenSize1, childrenSize2);
		if(max == 0)
			return 0;
		else
			return (double)mappedChildrenSize/(double)max;
	}
	
	private static boolean matchesOperation(OperationInvocation invocation, List<UMLOperation> operations) {
		for(UMLOperation operation : operations) {
			if(invocation.matchesOperation(operation))
				return true;
		}
		return false;
	}
}
