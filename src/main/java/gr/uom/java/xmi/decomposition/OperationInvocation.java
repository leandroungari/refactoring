package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.StringDistance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class OperationInvocation extends AbstractCall {
	private String methodName;
	private List<String> subExpressions = new ArrayList<String>();
	private volatile int hashCode = 0;
	
	public OperationInvocation(CompilationUnit cu, String filePath, MethodInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation);
		this.methodName = invocation.getName().getIdentifier();
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
		if(invocation.getExpression() != null) {
			this.expression = invocation.getExpression().toString();
			processExpression(invocation.getExpression(), this.subExpressions);
		}
	}
	
	private void processExpression(Expression expression, List<String> subExpressions) {
		if(expression instanceof MethodInvocation) {
			MethodInvocation invocation = (MethodInvocation)expression;
			if(invocation.getExpression() != null) {
				String expressionAsString = invocation.getExpression().toString();
				String invocationAsString = invocation.toString();
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(invocation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, invocation.toString());
			}
		}
		else if(expression instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation = (ClassInstanceCreation)expression;
			if(creation.getExpression() != null) {
				String expressionAsString = creation.getExpression().toString();
				String invocationAsString = creation.toString();
				String suffix = invocationAsString.substring(expressionAsString.length() + 1, invocationAsString.length());
				subExpressions.add(0, suffix);
				processExpression(creation.getExpression(), subExpressions);
			}
			else {
				subExpressions.add(0, creation.toString());
			}
		}
	}

	public OperationInvocation(CompilationUnit cu, String filePath, SuperMethodInvocation invocation) {
		this.locationInfo = new LocationInfo(cu, filePath, invocation);
		this.methodName = invocation.getName().getIdentifier();
		this.typeArguments = invocation.arguments().size();
		this.arguments = new ArrayList<String>();
		this.expression = "super";
		this.subExpressions.add("super");
		List<Expression> args = invocation.arguments();
		for(Expression argument : args) {
			this.arguments.add(argument.toString());
		}
	}

	private OperationInvocation() {
		
	}

	public OperationInvocation update(String oldExpression, String newExpression) {
		OperationInvocation newOperationInvocation = new OperationInvocation();
		newOperationInvocation.methodName = this.methodName;
		newOperationInvocation.locationInfo = this.locationInfo;
		update(newOperationInvocation, oldExpression, newExpression);
		newOperationInvocation.subExpressions = new ArrayList<String>();
		for(String argument : this.subExpressions) {
			newOperationInvocation.subExpressions.add(
				ReplacementUtil.performReplacement(argument, oldExpression, newExpression));
		}
		return newOperationInvocation;
	}

	public String getName() {
		return getMethodName();
	}

    public String getMethodName() {
		return methodName;
	}

    public int numberOfSubExpressions() {
    	return subExpressions.size();
    }

    public boolean matchesOperation(UMLOperation operation) {
    	List<String> inferredArgumentTypes = new ArrayList<String>();
    	for(String arg : arguments) {
    		if(arg.startsWith("\"") && arg.endsWith("\"")) {
    			inferredArgumentTypes.add("String");
    		}
    		else if(arg.endsWith(".class")) {
    			inferredArgumentTypes.add("Class");
    		}
    		else if(arg.equals("true")) {
    			inferredArgumentTypes.add("boolean");
    		}
    		else if(arg.equals("false")) {
    			inferredArgumentTypes.add("boolean");
    		}
    		else {
    			inferredArgumentTypes.add(null);
    		}
    	}
    	int i=0;
    	for(UMLType parameterType : operation.getParameterTypeList()) {
    		if(inferredArgumentTypes.size() > i && inferredArgumentTypes.get(i) != null) {
    			if(!parameterType.getClassType().equals(inferredArgumentTypes.get(i))) {
    				return false;
    			}
    		}
    		i++;
    	}
    	return this.methodName.equals(operation.getName()) && (this.typeArguments == operation.getParameterTypeList().size() || varArgsMatch(operation));
    }

    private boolean varArgsMatch(UMLOperation operation) {
    	//0 varargs arguments passed
    	return this.typeArguments == operation.getNumberOfNonVarargsParameters() ||
    			//>=1 varargs arguments passed
    			(operation.hasVarargsParameter() && this.typeArguments > operation.getNumberOfNonVarargsParameters());
    }

    public boolean compatibleExpression(OperationInvocation other) {
    	if(this.expression != null && other.expression != null) {
    		if(this.expression.startsWith("new ") && !other.expression.startsWith("new "))
    			return false;
    		if(!this.expression.startsWith("new ") && other.expression.startsWith("new "))
    			return false;
    	}
    	if(this.expression != null && this.expression.startsWith("new ") && other.expression == null)
    		return false;
    	if(other.expression != null && other.expression.startsWith("new ") && this.expression == null)
    		return false;
    	if(this.subExpressions.size() > 1 || other.subExpressions.size() > 1) {
    		Set<String> intersection = subExpressionIntersection(other);
    		int thisUnmatchedSubExpressions = this.subExpressions.size() - intersection.size();
    		int otherUnmatchedSubExpressions = other.subExpressions.size() - intersection.size();
    		if(thisUnmatchedSubExpressions > intersection.size() || otherUnmatchedSubExpressions > intersection.size())
    			return false;
    	}
    	return true;
    }

    public Set<String> callChainIntersection(OperationInvocation other) {
    	Set<String> s1 = new LinkedHashSet<String>(this.subExpressions);
    	s1.add(this.actualString());
    	Set<String> s2 = new LinkedHashSet<String>(other.subExpressions);
    	s1.add(other.actualString());

    	Set<String> intersection = new LinkedHashSet<String>(s1);
    	intersection.retainAll(s2);
    	return intersection;
    }

    private String actualString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(methodName);
    	sb.append("(");
    	int size = arguments.size();
    	if(size > 0) {
    		for(int i=0; i<size-1; i++)
    			sb.append(arguments.get(i)).append(",");
    		sb.append(arguments.get(size-1));
    	}
    	sb.append(")");
    	return sb.toString();
    }

    private Set<String> subExpressionIntersection(OperationInvocation other) {
    	Set<String> intersection = new LinkedHashSet<String>(this.subExpressions);
    	intersection.retainAll(other.subExpressions);
    	return intersection;
    }

	public double normalizedNameDistance(AbstractCall call) {
		String s1 = getMethodName().toLowerCase();
		String s2 = ((OperationInvocation)call).getMethodName().toLowerCase();
		int distance = StringDistance.editDistance(s1, s2);
		double normalized = (double)distance/(double)Math.max(s1.length(), s2.length());
		return normalized;
	}

	public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if (o instanceof OperationInvocation) {
        	OperationInvocation invocation = (OperationInvocation)o;
            return methodName.equals(invocation.methodName) &&
                typeArguments == invocation.typeArguments;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append("(");
        if(typeArguments > 0) {
            for(int i=0; i<typeArguments-1; i++)
                sb.append("arg" + i).append(", ");
            sb.append("arg" + (typeArguments-1));
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + methodName.hashCode();
    		result = 37*result + typeArguments;
    		hashCode = result;
    	}
    	return hashCode;
    }

	public boolean identicalName(AbstractCall call) {
		return getMethodName().equals(((OperationInvocation)call).getMethodName());
	}

	public boolean typeInferenceMatch(UMLOperation operationToBeMatched, Map<String, UMLType> typeInferenceMapFromContext) {
		List<UMLParameter> parameters = operationToBeMatched.getParametersWithoutReturnType();
		if(operationToBeMatched.hasVarargsParameter()) {
			//we expect arguments to be =(parameters-1), or =parameters, or >parameters
			if(getArguments().size() < parameters.size()) {
				int i = 0;
				for(String argument : getArguments()) {
					if(typeInferenceMapFromContext.containsKey(argument)) {
						UMLType argumentType = typeInferenceMapFromContext.get(argument);
						UMLType paremeterType = parameters.get(i).getType();
						if(!argumentType.equals(paremeterType))
							return false;
					}
					i++;
				}
			}
			else {
				int i = 0;
				for(UMLParameter parameter : parameters) {
					String argument = getArguments().get(i);
					if(typeInferenceMapFromContext.containsKey(argument)) {
						UMLType argumentType = typeInferenceMapFromContext.get(argument);
						UMLType paremeterType = parameter.isVarargs() ?
								UMLType.extractTypeObject(parameter.getType().getClassType()) :
								parameter.getType();
						if(!argumentType.equals(paremeterType))
							return false;
					}
					i++;
				}
			}
			
		}
		else {
			//we expect an equal number of parameters and arguments
			int i = 0;
			for(String argument : getArguments()) {
				if(typeInferenceMapFromContext.containsKey(argument)) {
					UMLType argumentType = typeInferenceMapFromContext.get(argument);
					UMLType paremeterType = parameters.get(i).getType();
					if(!argumentType.equals(paremeterType))
						return false;
				}
				i++;
			}
		}
		return true;
	}
}
