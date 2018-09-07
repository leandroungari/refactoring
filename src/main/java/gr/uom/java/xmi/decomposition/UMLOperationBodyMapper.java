package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ObjectCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.diff.CandidateAttributeRename;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.api.Refactoring;

public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper> {
	private UMLOperation operation1;
	private UMLOperation operation2;
	private boolean isInitialized = true;
	private List<AbstractCodeMapping> mappings;
	private List<StatementObject> nonMappedLeavesT1;
	private List<StatementObject> nonMappedLeavesT2;
	private List<CompositeStatementObject> nonMappedInnerNodesT1;
	private List<CompositeStatementObject> nonMappedInnerNodesT2;
	private Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
	private Set<CandidateAttributeRename> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRename>();
	private List<UMLOperationBodyMapper> additionalMappers = new ArrayList<UMLOperationBodyMapper>();
	private static final Pattern SPLIT_CONDITIONAL_PATTERN = Pattern.compile("(\\|\\|)|(&&)|(\\?)|(:)");
	private static final double MAX_ANONYMOUS_CLASS_DECLARATION_DISTANCE = 0.2;
	private UMLClassBaseDiff classDiff;
	
	public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2) {
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		this.isInitialized = false;
	}

	public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2, UMLClassBaseDiff classDiff) {
		this(operation1, operation2);
		this.classDiff = classDiff;
	}

	public void addAdditionalMapper(UMLOperationBodyMapper mapper) {
		this.additionalMappers.add(mapper);
		//TODO add logic to remove the mappings from "this" mapper,
		//which are less similar than the mappings of the mapper passed as parameter
	}

	public List<UMLOperationBodyMapper> getAdditionalMappers() {
		return additionalMappers;
	}

	private void initialize() {
		if (this.isInitialized) {
			return;
		}
		this.isInitialized = true;
		OperationBody body1 = operation1.getBody();
		OperationBody body2 = operation2.getBody();
		if(body1 != null && body2 != null) {
			CompositeStatementObject composite1 = body1.getCompositeStatement();
			CompositeStatementObject composite2 = body2.getCompositeStatement();
			List<StatementObject> leaves1 = composite1.getLeaves();
			List<StatementObject> leaves2 = composite2.getLeaves();
			
			UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2);
			Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<String, String>();
			Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<String, String>();
			List<UMLParameter> addedParameters = operationDiff.getAddedParameters();
			if(addedParameters.size() == 1) {
				UMLParameter addedParameter = addedParameters.get(0);
				if(UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName())) {
					parameterToArgumentMap1.put("this.", "");
					//replace "parameterName." with ""
					parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
				}
			}
			List<UMLParameter> removedParameters = operationDiff.getRemovedParameters();
			if(removedParameters.size() == 1) {
				UMLParameter removedParameter = removedParameters.get(0);
				if(UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
					parameterToArgumentMap2.put("this.", "");
				}
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(StatementObject leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(StatementObject leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			processLeaves(leaves1, leaves2, new LinkedHashMap<String, String>());
			
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNodes2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<String, String>());
			
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	private void resetNodes(List<? extends AbstractCodeFragment> nodes) {
		for(AbstractCodeFragment node : nodes) {
			node.resetArgumentization();
		}
	}
	
	public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2) {
		this.operation1 = operationBodyMapper.operation1;
		this.operation2 = addedOperation;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody addedOperationBody = addedOperation.getBody();
		if(addedOperationBody != null) {
			CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
			List<StatementObject> leaves1 = operationBodyMapper.getNonMappedLeavesT1();
			//adding leaves that were mapped with replacements
			Set<StatementObject> addedLeaves1 = new LinkedHashSet<StatementObject>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					if(fragment instanceof StatementObject) {
						StatementObject statement = (StatementObject)fragment;
						if(!leaves1.contains(statement)) {
							leaves1.add(statement);
							addedLeaves1.add(statement);
						}
						if(!statement.getAnonymousClassDeclarations().isEmpty()) {
							List<UMLAnonymousClass> anonymousList = operationBodyMapper.getOperation1().getAnonymousClassList();
							for(UMLAnonymousClass anonymous : anonymousList) {
								if(statement.getLocationInfo().subsumes(anonymous.getLocationInfo())) {
									for(UMLOperation anonymousOperation : anonymous.getOperations()) {
										List<StatementObject> anonymousClassLeaves = anonymousOperation.getBody().getCompositeStatement().getLeaves();
										for(StatementObject anonymousLeaf : anonymousClassLeaves) {
											if(!leaves1.contains(anonymousLeaf)) {
												leaves1.add(anonymousLeaf);
												addedLeaves1.add(anonymousLeaf);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			List<StatementObject> leaves2 = composite2.getLeaves();
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(StatementObject leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(StatementObject leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap2);

			List<CompositeStatementObject> innerNodes1 = operationBodyMapper.getNonMappedInnerNodesT1();
			//adding innerNodes that were mapped with replacements
			Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject statement = (CompositeStatementObject)fragment;
						if(!innerNodes1.contains(statement)) {
							innerNodes1.add(statement);
							addedInnerNodes1.add(statement);
						}
					}
				}
			}
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNode2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap2);
			
			//match expressions in inner nodes from T1 with leaves from T2
			List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT1.add(expression);
				}
			}
			processLeaves(expressionsT1, leaves2, parameterToArgumentMap2);
			// TODO remove non-mapped inner nodes from T1 corresponding to mapped expressions
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes1.removeAll(addedInnerNodes1);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper operationBodyMapper,
			Map<String, String> parameterToArgumentMap) {
		this.operation1 = removedOperation;
		this.operation2 = operationBodyMapper.operation2;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody removedOperationBody = removedOperation.getBody();
		if(removedOperationBody != null) {
			CompositeStatementObject composite1 = removedOperationBody.getCompositeStatement();
			List<StatementObject> leaves1 = composite1.getLeaves();
			List<StatementObject> leaves2 = operationBodyMapper.getNonMappedLeavesT2();
			//adding leaves that were mapped with replacements or are inexact matches
			Set<StatementObject> addedLeaves2 = new LinkedHashSet<StatementObject>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof StatementObject) {
						StatementObject statement = (StatementObject)fragment;
						if(!leaves2.contains(statement)) {
							leaves2.add(statement);
							addedLeaves2.add(statement);
						}
					}
				}
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap.isEmpty()) {
				//check for temporary variables that the argument might be assigned to
				for(StatementObject leave2 : leaves2) {
					List<VariableDeclaration> variableDeclarations = leave2.getVariableDeclarations();
					for(VariableDeclaration variableDeclaration : variableDeclarations) {
						for(String parameter : parameterToArgumentMap.keySet()) {
							String argument = parameterToArgumentMap.get(parameter);
							if(argument.equals(variableDeclaration.getInitializer())) {
								parameterToArgumentMap.put(parameter, variableDeclaration.getVariableName());
							}
						}
					}
				}
				for(StatementObject leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap);
			
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = operationBodyMapper.getNonMappedInnerNodesT2();
			//adding innerNodes that were mapped with replacements or are inexact matches
			Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject statement = (CompositeStatementObject)fragment;
						if(!innerNodes2.contains(statement)) {
							innerNodes2.add(statement);
							addedInnerNodes2.add(statement);
						}
					}
				}
			}
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap);
				}
			}
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap);
			
			//match expressions in inner nodes from T2 with leaves from T1
			List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT2.add(expression);
				}
			}
			processLeaves(leaves1, expressionsT2, parameterToArgumentMap);
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes2.removeAll(addedInnerNodes2);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	public UMLOperation getOperation1() {
		return operation1;
	}

	public UMLOperation getOperation2() {
		return operation2;
	}

	public Set<Refactoring> getRefactorings() {
		initialize();
		refactorings.addAll(findConsistentVariableRenames());
		return refactorings;
	}

	public Set<CandidateAttributeRename> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	public List<AbstractCodeMapping> getMappings() {
		initialize();
		return mappings;
	}

	public List<StatementObject> getNonMappedLeavesT1() {
		initialize();
		return nonMappedLeavesT1;
	}

	public List<StatementObject> getNonMappedLeavesT2() {
		initialize();
		return nonMappedLeavesT2;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT1() {
		initialize();
		return nonMappedInnerNodesT1;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT2() {
		initialize();
		return nonMappedInnerNodesT2;
	}

	public int mappingsWithoutBlocks() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(countableStatement(mapping.getFragment1()))
				count++;
		}
		return count;
	}

	public int nonMappedElementsT1() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(countableStatement(composite))
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT1()) {
			if(countableStatement(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT1() {
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT1()) {
			if(countableStatement(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	public int nonMappedElementsT2() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(countableStatement(composite))
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT2()) {
			if(countableStatement(statement) && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT2() {
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT2()) {
			if(countableStatement(statement) && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	private boolean isTemporaryVariableAssignment(StatementObject statement) {
		for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
			for(Replacement replacement : getReplacements()) {
				String variableName = declaration.getVariableName();
				String initializer = declaration.getInitializer();
				if(variableName.equals(replacement.getAfter()) &&
						initializer != null && initializer.equals(replacement.getBefore())) {
					ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation2);
					if(!refactorings.contains(ref)) {
						refactorings.add(ref);
					}
					return true;
				}
			}
		}
		String argumentizedString = statement.getArgumentizedString();
		if(argumentizedString.contains("=")) {
			String beforeAssignment = argumentizedString.substring(0, argumentizedString.indexOf("="));
			String[] tokens = beforeAssignment.split("\\s");
			String variable = tokens[tokens.length-1];
			String initializer = argumentizedString.substring(argumentizedString.indexOf("=")+1, argumentizedString.length()-2);
			for(Replacement replacement : getReplacements()) {
				if(variable.endsWith(replacement.getAfter()) &&
						initializer.equals(replacement.getBefore())) {
					List<VariableDeclaration> variableDeclarations = operation2.getAllVariableDeclarations();
					for(VariableDeclaration declaration : variableDeclarations) {
						if(declaration.getVariableName().equals(variable)) {
							ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation2);
							if(!refactorings.contains(ref)) {
								refactorings.add(ref);
							}
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public int nonMappedElementsT2CallingAddedOperation(List<UMLOperation> addedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(countableStatement(composite)) {
				Map<String, OperationInvocation> methodInvocationMap = composite.getMethodInvocationMap();
				for(OperationInvocation invocation : methodInvocationMap.values()) {
					for(UMLOperation operation : addedOperations) {
						if(invocation.matchesOperation(operation)) {
							nonMappedInnerNodeCount++;
							break;
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT2()) {
			if(countableStatement(statement)) {
				Map<String, OperationInvocation> methodInvocationMap = statement.getMethodInvocationMap();
				for(OperationInvocation invocation : methodInvocationMap.values()) {
					for(UMLOperation operation : addedOperations) {
						if(invocation.matchesOperation(operation)) {
							nonMappedLeafCount++;
							break;
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedElementsT1CallingRemovedOperation(List<UMLOperation> removedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(countableStatement(composite)) {
				Map<String, OperationInvocation> methodInvocationMap = composite.getMethodInvocationMap();
				for(OperationInvocation invocation : methodInvocationMap.values()) {
					for(UMLOperation operation : removedOperations) {
						if(invocation.matchesOperation(operation)) {
							nonMappedInnerNodeCount++;
							break;
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(StatementObject statement : getNonMappedLeavesT1()) {
			if(countableStatement(statement)) {
				Map<String, OperationInvocation> methodInvocationMap = statement.getMethodInvocationMap();
				for(OperationInvocation invocation : methodInvocationMap.values()) {
					for(UMLOperation operation : removedOperations) {
						if(invocation.matchesOperation(operation)) {
							nonMappedLeafCount++;
							break;
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public boolean callsRemovedAndAddedOperation(List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		boolean removedOperationCalled = false;
		for(OperationInvocation invocation : operation1.getAllOperationInvocations()) {
			for(UMLOperation operation : removedOperations) {
				if(invocation.matchesOperation(operation)) {
					removedOperationCalled = true;
					break;
				}
			}
			if(removedOperationCalled)
				break;
		}
		boolean addedOperationCalled = false;
		for(OperationInvocation invocation : operation2.getAllOperationInvocations()) {
			for(UMLOperation operation : addedOperations) {
				if(invocation.matchesOperation(operation)) {
					addedOperationCalled = true;
					break;
				}
			}
			if(addedOperationCalled)
				break;
		}
		return removedOperationCalled && addedOperationCalled;
	}

	public int exactMatches() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && countableStatement(mapping.getFragment1()) &&
					!mapping.getFragment1().getString().equals("try"))
				count++;
		}
		return count;
	}

	public List<AbstractCodeMapping> getExactMatches() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<AbstractCodeMapping>();
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && countableStatement(mapping.getFragment1()) &&
					!mapping.getFragment1().getString().equals("try"))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	private boolean countableStatement(AbstractCodeFragment fragment) {
		String statement = fragment.getString();
		//covers the cases of methods with only one statement in their body
		if(fragment instanceof AbstractStatement && ((AbstractStatement)fragment).getParent().statementCount() == 1 && ((AbstractStatement)fragment).getParent().getParent() == null) {
			return true;
		}
		return !statement.equals("{") && !statement.startsWith("catch(") && !statement.startsWith("case ") && !statement.startsWith("default :") &&
				!statement.startsWith("return true;") && !statement.startsWith("return false;") && !statement.startsWith("return this;") && !statement.startsWith("return null;") && !statement.startsWith("return;");
	}

	private int editDistance() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
			String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
			if(!s1.equals(s2)) {
				count += StringDistance.editDistance(s1, s2);
			}
		}
		return count;
	}

	private int operationNameEditDistance() {
		return StringDistance.editDistance(this.operation1.getName(), this.operation2.getName());
	}

	public Set<Replacement> getReplacements() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			replacements.addAll(mapping.getReplacements());
		}
		return replacements;
	}

	public Map<Replacement, Integer> getReplacementOccurrenceMap(ReplacementType type) {
		Map<Replacement, Integer> map = new LinkedHashMap<Replacement, Integer>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(type)) {
					if(map.containsKey(replacement)) {
						int count = map.get(replacement);
						map.put(replacement, count+1);
					}
					else {
						map.put(replacement, 1);
					}
				}
			}
		}
		return map;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof MethodInvocationReplacement ||
						replacement instanceof VariableReplacementWithMethodInvocation ||
						replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION)) {
					replacements.add(replacement);
				}
			}
		}
		return replacements;
	}

	public Set<MethodInvocationReplacement> getMethodInvocationRenameReplacements() {
		Set<MethodInvocationReplacement> replacements = new LinkedHashSet<MethodInvocationReplacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT)) {
					replacements.add((MethodInvocationReplacement) replacement);
				}
			}
		}
		return replacements;
	}

	public void processInnerNodes(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2,
			Map<String, String> parameterToArgumentMap) {
		List<UMLOperation> removedOperations = classDiff != null ? classDiff.getRemovedOperations() : new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperations = classDiff != null ? classDiff.getAddedOperations() : new ArrayList<UMLOperation>();
		//exact string+depth matching - inner nodes
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				double score = statement1.compositeChildMatchingScore(statement2, mappings, removedOperations, addedOperations);
				if((statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
						statement1.getDepth() == statement2.getDepth() &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		//exact string matching - inner nodes - finds moves to another level
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				double score = statement1.compositeChildMatchingScore(statement2, mappings, removedOperations, addedOperations);
				if((statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		// exact matching - inner nodes - with variable renames
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap);
				
				double score = statement1.compositeChildMatchingScore(statement2, mappings, removedOperations, addedOperations);
				if(replacements != null &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mapping.addReplacements(replacements);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
	}

	public void processLeaves(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap) {
		if(leaves1.size() <= leaves2.size()) {
			//exact string+depth matching - leaf nodes
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
					AbstractCodeFragment leaf2 = leafIterator2.next();
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves2.remove(minStatementMapping.getFragment2());
					leafIterator1.remove();
				}
			}
			
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
					AbstractCodeFragment leaf2 = leafIterator2.next();
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves2.remove(minStatementMapping.getFragment2());
					leafIterator1.remove();
				}
			}
			
			// exact matching with variable renames
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
					AbstractCodeFragment leaf2 = leafIterator2.next();
					
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap);
					if (replacements != null) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mapping.addReplacements(replacements);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves2.remove(minStatementMapping.getFragment2());
					leafIterator1.remove();
				}
			}
		}
		else {
			//exact string+depth matching - leaf nodes
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
					AbstractCodeFragment leaf1 = leafIterator1.next();
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves1.remove(minStatementMapping.getFragment1());
					leafIterator2.remove();
				}
			}
			
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
					AbstractCodeFragment leaf1 = leafIterator1.next();
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves1.remove(minStatementMapping.getFragment1());
					leafIterator2.remove();
				}
			}
			
			// exact matching with variable renames
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
				for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
					AbstractCodeFragment leaf1 = leafIterator1.next();
					
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap);
					if (replacements != null) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mapping.addReplacements(replacements);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves1.remove(minStatementMapping.getFragment1());
					leafIterator2.remove();
				}
			}
		}
	}

	private String preprocessInput1(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf1, leaf2);
	}

	private String preprocessInput2(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf2, leaf1);
	}

	private String preprocessInput(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		String argumentizedString = new String(leaf1.getArgumentizedString());
		if (leaf1 instanceof StatementObject && leaf2 instanceof AbstractExpression) {
			if (argumentizedString.startsWith("return ") && argumentizedString.endsWith(";\n")) {
				argumentizedString = argumentizedString.substring("return ".length(),
						argumentizedString.lastIndexOf(";\n"));
			}
		}
		return argumentizedString;
	}

	private static class ReplacementInfo {
		private String argumentizedString1;
		private String argumentizedString2;
		private int rawDistance;
		private Set<Replacement> replacements;
		
		public ReplacementInfo(String argumentizedString1, String argumentizedString2) {
			this.argumentizedString1 = argumentizedString1;
			this.argumentizedString2 = argumentizedString2;
			this.rawDistance = StringDistance.editDistance(argumentizedString1, argumentizedString2);
			this.replacements = new LinkedHashSet<Replacement>();
		}
		public String getArgumentizedString1() {
			return argumentizedString1;
		}
		public String getArgumentizedString2() {
			return argumentizedString2;
		}
		public void setArgumentizedString1(String string) {
			this.argumentizedString1 = string;
			this.rawDistance = StringDistance.editDistance(this.argumentizedString1, this.argumentizedString2);
		}
		public int getRawDistance() {
			return rawDistance;
		}
		public void addReplacement(Replacement r) {
			this.replacements.add(r);
		}
		public void addReplacements(Set<Replacement> replacementsToBeAdded) {
			this.replacements.addAll(replacementsToBeAdded);
		}
		public void removeReplacements(Set<Replacement> replacementsToBeRemoved) {
			this.replacements.removeAll(replacementsToBeRemoved);
		}
		public Set<Replacement> getReplacements() {
			return replacements;
		}
		public List<Replacement> getReplacements(ReplacementType type) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			for(Replacement replacement : this.replacements) {
				if(replacement.getType().equals(type)) {
					replacements.add(replacement);
				}
			}
			return replacements;
		}
	}

	private Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap) {
		List<VariableDeclaration> variableDeclarations1 = new ArrayList<VariableDeclaration>(statement1.getVariableDeclarations());
		List<VariableDeclaration> variableDeclarations2 = new ArrayList<VariableDeclaration>(statement2.getVariableDeclarations());
		Set<String> variables1 = new LinkedHashSet<String>(statement1.getVariables());
		Set<String> variables2 = new LinkedHashSet<String>(statement2.getVariables());
		Set<String> variableIntersection = new LinkedHashSet<String>(variables1);
		variableIntersection.retainAll(variables2);
		// ignore the variables in the intersection that also appear with "this." prefix in the sets of variables
		// ignore the variables in the intersection that are static fields
		Set<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
		for(String variable : variableIntersection) {
			if(!variable.startsWith("this.") && !variableIntersection.contains("this."+variable) &&
					(variables1.contains("this."+variable) || variables2.contains("this."+variable))) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			if(variable.toUpperCase().equals(variable)) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
		}
		variableIntersection.removeAll(variablesToBeRemovedFromTheIntersection);
		// remove common variables from the two sets
		variables1.removeAll(variableIntersection);
		variables2.removeAll(variableIntersection);
		
		// replace variables with the corresponding arguments
		replaceVariablesWithArguments(variables1, parameterToArgumentMap);
		replaceVariablesWithArguments(variables2, parameterToArgumentMap);
		
		Map<String, AbstractCall> methodInvocationMap1 = new LinkedHashMap<String, AbstractCall>(statement1.getMethodInvocationMap());
		Map<String, AbstractCall> methodInvocationMap2 = new LinkedHashMap<String, AbstractCall>(statement2.getMethodInvocationMap());
		Set<String> methodInvocations1 = new LinkedHashSet<String>(methodInvocationMap1.keySet());
		Set<String> methodInvocations2 = new LinkedHashSet<String>(methodInvocationMap2.keySet());
		
		// replace variables with the corresponding arguments in method invocations
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, parameterToArgumentMap);
		replaceVariablesWithArguments(methodInvocationMap2, methodInvocations2, parameterToArgumentMap);
		
		OperationInvocation invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
		OperationInvocation invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
		//remove methodInvocation covering the entire statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String methodInvocation1 : methodInvocationMap1.keySet()) {
				if(invocationCoveringTheEntireStatement1.getLocationInfo().equals(methodInvocationMap1.get(methodInvocation1).getLocationInfo())) {
					methodInvocations1.remove(methodInvocation1);
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation2 : methodInvocationMap2.keySet()) {
				if(invocationCoveringTheEntireStatement2.getLocationInfo().equals(methodInvocationMap2.get(methodInvocation2).getLocationInfo())) {
					methodInvocations2.remove(methodInvocation2);
				}
			}
		}
		Set<String> methodInvocationIntersection = new LinkedHashSet<String>(methodInvocations1);
		methodInvocationIntersection.retainAll(methodInvocations2);
		// remove common methodInvocations from the two sets
		methodInvocations1.removeAll(methodInvocationIntersection);
		methodInvocations2.removeAll(methodInvocationIntersection);
		
		Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<String>();
		variablesAndMethodInvocations1.addAll(methodInvocations1);
		variablesAndMethodInvocations1.addAll(variables1);
		
		Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<String>();
		variablesAndMethodInvocations2.addAll(methodInvocations2);
		variablesAndMethodInvocations2.addAll(variables2);
		
		Set<String> types1 = new LinkedHashSet<String>(statement1.getTypes());
		Set<String> types2 = new LinkedHashSet<String>(statement2.getTypes());
		Set<String> typeIntersection = new LinkedHashSet<String>(types1);
		typeIntersection.retainAll(types2);
		// remove common types from the two sets
		types1.removeAll(typeIntersection);
		types2.removeAll(typeIntersection);
		
		Map<String, AbstractCall> creationMap1 = new LinkedHashMap<String, AbstractCall>(statement1.getCreationMap());
		Map<String, AbstractCall> creationMap2 = new LinkedHashMap<String, AbstractCall>(statement2.getCreationMap());
		Set<String> creations1 = new LinkedHashSet<String>(creationMap1.keySet());
		Set<String> creations2 = new LinkedHashSet<String>(creationMap2.keySet());
		
		// replace variables with the corresponding arguments in object creations
		replaceVariablesWithArguments(creationMap1, creations1, parameterToArgumentMap);
		replaceVariablesWithArguments(creationMap2, creations2, parameterToArgumentMap);
		
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		//remove objectCreation covering the entire statement
		for(String objectCreation1 : creationMap1.keySet()) {
			ObjectCreation creation1 = (ObjectCreation) creationMap1.get(objectCreation1);
			if(creationCoveringTheEntireStatement1 != null && 
					creationCoveringTheEntireStatement1.getLocationInfo().equals(creation1.getLocationInfo())) {
				creations1.remove(objectCreation1);
			}
			if(creation1.getAnonymousClassDeclaration() != null) {
				creations1.remove(objectCreation1);
			}
		}
		for(String objectCreation2 : creationMap2.keySet()) {
			ObjectCreation creation2 = (ObjectCreation) creationMap2.get(objectCreation2);
			if(creationCoveringTheEntireStatement2 != null &&
					creationCoveringTheEntireStatement2.getLocationInfo().equals(creation2.getLocationInfo())) {
				creations2.remove(objectCreation2);
			}
			if(creation2.getAnonymousClassDeclaration() != null) {
				creations2.remove(objectCreation2);
			}
		}
		Set<String> creationIntersection = new LinkedHashSet<String>(creations1);
		creationIntersection.retainAll(creations2);
		// remove common creations from the two sets
		creations1.removeAll(creationIntersection);
		creations2.removeAll(creationIntersection);
		
		Set<String> stringLiterals1 = new LinkedHashSet<String>(statement1.getStringLiterals());
		Set<String> stringLiterals2 = new LinkedHashSet<String>(statement2.getStringLiterals());
		Set<String> stringLiteralIntersection = new LinkedHashSet<String>(stringLiterals1);
		stringLiteralIntersection.retainAll(stringLiterals2);
		// remove common string literals from the two sets
		stringLiterals1.removeAll(stringLiteralIntersection);
		stringLiterals2.removeAll(stringLiteralIntersection);
		
		Set<String> infixOperators1 = new LinkedHashSet<String>(statement1.getInfixOperators());
		Set<String> infixOperators2 = new LinkedHashSet<String>(statement2.getInfixOperators());
		Set<String> infixOperatorIntersection = new LinkedHashSet<String>(infixOperators1);
		infixOperatorIntersection.retainAll(infixOperators2);
		// remove common infix operators from the two sets
		infixOperators1.removeAll(infixOperatorIntersection);
		infixOperators2.removeAll(infixOperatorIntersection);
		
		ReplacementInfo replacementInfo = new ReplacementInfo(
				preprocessInput1(statement1, statement2),
				preprocessInput2(statement1, statement2));
		
		//perform type replacements
		findReplacements(types1, types2, replacementInfo, ReplacementType.TYPE);
		
		//perform operator replacements
		findReplacements(infixOperators1, infixOperators2, replacementInfo, ReplacementType.INFIX_OPERATOR);
		
		if (replacementInfo.getRawDistance() > 0) {
			for(String s1 : variablesAndMethodInvocations1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				int minDistance = replacementInfo.getRawDistance();
				for(String s2 : variablesAndMethodInvocations2) {
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2, variablesAndMethodInvocations1, variablesAndMethodInvocations2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						minDistance = distanceRaw;
						Replacement replacement = null;
						if(variables1.contains(s1) && variables2.contains(s2) && variablesStartWithSameCase(s1, s2, parameterToArgumentMap)) {
							replacement = new Replacement(s1, s2, ReplacementType.VARIABLE_NAME);
						}
						else if(variables1.contains(s1) && methodInvocations2.contains(s2)) {
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, (OperationInvocation) methodInvocationMap2.get(s2));
						}
						else if(methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
							OperationInvocation invokedOperationBefore = (OperationInvocation) methodInvocationMap1.get(s1);
							OperationInvocation invokedOperationAfter = (OperationInvocation) methodInvocationMap2.get(s2);
							if(invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
								replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION);
							}
						}
						else if(methodInvocations1.contains(s1) && variables2.contains(s2)) {
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, (OperationInvocation) methodInvocationMap1.get(s1));
						}
						if(replacement != null) {
							double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
							replacementMap.put(distancenormalized, replacement);
						}
						if(distanceRaw == 0 && !replacementInfo.getReplacements().isEmpty()) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Replacement replacement = replacementMap.firstEntry().getValue();
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(),
							replacement.getBefore(), replacement.getAfter(), variablesAndMethodInvocations1, variablesAndMethodInvocations2));
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		
		//perform creation replacements
		findReplacements(creations1, creations2, replacementInfo, ReplacementType.CLASS_INSTANCE_CREATION);
		
		//perform string literal replacements
		if(!containsMethodInvocationReplacement(replacementInfo.getReplacements())) {
			findReplacements(stringLiterals1, stringLiterals2, replacementInfo, ReplacementType.STRING_LITERAL);
		}
		
		String s1 = preprocessInput1(statement1, statement2);
		String s2 = preprocessInput2(statement1, statement2);
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<Replacement>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			s1 = ReplacementUtil.performReplacement(s1, s2, replacement.getBefore(), replacement.getAfter(), variablesAndMethodInvocations1, variablesAndMethodInvocations2);
			//find variable replacements within method invocation replacements
			Replacement r = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, variables2);
			if(r != null) {
				replacementsToBeRemoved.add(replacement);
				replacementsToBeAdded.add(r);
			}
		}
		replacementInfo.removeReplacements(replacementsToBeRemoved);
		replacementInfo.addReplacements(replacementsToBeAdded);
		boolean isEqualWithReplacement = s1.equals(s2) || (commonConditional(s1, s2) && containsValidOperatorReplacements(replacementInfo));
		if(isEqualWithReplacement) {
			if(variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
				boolean typeReplacement = false, variableRename = false, methodInvocationReplacement = false;
				for(Replacement replacement : replacementInfo.getReplacements()) {
					if(replacement.getType().equals(ReplacementType.TYPE))
						typeReplacement = true;
					else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME))
						variableRename = true;
					else if(replacement instanceof MethodInvocationReplacement)
						methodInvocationReplacement = true;
				}
				if(typeReplacement && variableRename && methodInvocationReplacement) {
					return null;
				}
			}
			return replacementInfo.getReplacements();
		}
		List<String> anonymousClassDeclarations1 = statement1.getAnonymousClassDeclarations();
		List<String> anonymousClassDeclarations2 = statement2.getAnonymousClassDeclarations();
		if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty() &&
				anonymousClassDeclarations1.size() == anonymousClassDeclarations2.size()) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				String anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
				String anonymousClassDeclaration2 = anonymousClassDeclarations2.get(i);
				int indexOfAnonymousClassDeclaration1 = statement1.getString().indexOf(anonymousClassDeclaration1);
				int indexOfAnonymousClassDeclaration2 = statement2.getString().indexOf(anonymousClassDeclaration2);
				if(indexOfAnonymousClassDeclaration1 != -1 && indexOfAnonymousClassDeclaration2 != -1) {
					String statementWithoutAnonymous1 = statement1.getString().substring(0, indexOfAnonymousClassDeclaration1);
					String statementWithoutAnonymous2 = statement2.getString().substring(0, indexOfAnonymousClassDeclaration2);
					if(statementWithoutAnonymous1.equals(statementWithoutAnonymous2)) {
						int editDistance = StringDistance.editDistance(anonymousClassDeclaration1, anonymousClassDeclaration2);
						double distancenormalized = (double)editDistance/(double)Math.max(anonymousClassDeclaration1.length(), anonymousClassDeclaration2.length());
						if(distancenormalized < MAX_ANONYMOUS_CLASS_DECLARATION_DISTANCE) {
							Replacement replacement = new Replacement(anonymousClassDeclaration1, anonymousClassDeclaration2, ReplacementType.ANONYMOUS_CLASS_DECLARATION);
							replacementInfo.addReplacement(replacement);
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		//method invocation is identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				AbstractCall invocation1 = methodInvocationMap1.get(key1);
				if(invocation1.identical(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
						!invocationCoveringTheEntireStatement1.getArguments().contains(key1)) {
					return replacementInfo.getReplacements();
				}
			}
		}
		//method invocation is identical if arguments are replaced
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(AbstractCall invocation2 : methodInvocationMap2.values()) {
				if(invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocation2, replacementInfo.getReplacements())) {
					return replacementInfo.getReplacements();
				}
			}
		}	
		//method invocation has been renamed but the expression and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed but the expressions are null and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalArgumentsAndNoExpression(invocationCoveringTheEntireStatement2, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed and arguments changed, but the expressions are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				OperationInvocation operationInvocation1 = (OperationInvocation) methodInvocationMap1.get(methodInvocation1);
				if(operationInvocation1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE)) {
					Replacement replacement = new MethodInvocationReplacement(operationInvocation1.getName(),
							invocationCoveringTheEntireStatement2.getName(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//method invocation has only changes in the arguments
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.onlyArgumentsChanged(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
			Set<String> argumentIntersection = invocationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2);
			if(!argumentIntersection.isEmpty() || invocationCoveringTheEntireStatement1.getArguments().size() == 0 || invocationCoveringTheEntireStatement2.getArguments().size() == 0) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				OperationInvocation operationInvocation1 = (OperationInvocation) methodInvocationMap1.get(methodInvocation1);
				if(operationInvocation1.onlyArgumentsChanged(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
					Set<String> argumentIntersection = operationInvocation1.argumentIntersection(invocationCoveringTheEntireStatement2);
					if(!argumentIntersection.isEmpty() || operationInvocation1.getArguments().size() == 0 || invocationCoveringTheEntireStatement2.getArguments().size() == 0) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.getName(),
								invocationCoveringTheEntireStatement2.getName(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the first statement is returned in the second statement
		Replacement r;
		if(invocationCoveringTheEntireStatement1 != null && (r = invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation1 : methodInvocations1) {
			AbstractCall operationInvocation1 = methodInvocationMap1.get(methodInvocation1);
			if(statement1.getString().endsWith(methodInvocation1 + ";\n") && (r = operationInvocation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		//check if the argument of the method call in the second statement is the right hand side of an assignment in the first statement
		if(invocationCoveringTheEntireStatement2 != null &&
				(r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedArgument(replacementInfo.getArgumentizedString1())) != null &&
				methodInvocationMap1.containsKey(invocationCoveringTheEntireStatement2.getArguments().get(0))) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		//check if the method call in the second statement is the expression of the method invocation in the first statement
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				OperationInvocation invocation1 = (OperationInvocation) methodInvocationMap1.get(key1);
				if(statement1.getString().endsWith(key1 + ";\n") &&
						methodInvocationMap2.keySet().contains(invocation1.getExpression())) {
					Replacement replacement = new MethodInvocationReplacement(invocation1.getName(),
							invocationCoveringTheEntireStatement2.getName(), invocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//check if the method call in the first statement is the expression of the method invocation in the second statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				OperationInvocation invocation2 = (OperationInvocation) methodInvocationMap2.get(key2);
				if(statement2.getString().endsWith(key2 + ";\n") &&
						methodInvocationMap1.keySet().contains(invocation2.getExpression())) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
							invocation2.getName(), invocationCoveringTheEntireStatement1, invocation2, ReplacementType.METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation has only changes in the arguments
		if(!creations1.isEmpty() && creationCoveringTheEntireStatement2 != null) {
			for(String creation1 : creations1) {
				ObjectCreation objectCreation1 = (ObjectCreation) creationMap1.get(creation1);
				if(objectCreation1.identicalName(creationCoveringTheEntireStatement2) &&
						!objectCreation1.equalArguments(creationCoveringTheEntireStatement2) &&
						objectCreation1.getArguments().size() != creationCoveringTheEntireStatement2.getArguments().size()) {
					Set<String> argumentIntersection = new LinkedHashSet<String>(objectCreation1.getArguments());
					argumentIntersection.retainAll(creationCoveringTheEntireStatement2.getArguments());
					if(!argumentIntersection.isEmpty() || objectCreation1.getArguments().size() == 0 || creationCoveringTheEntireStatement2.getArguments().size() == 0) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.getName(),
								creationCoveringTheEntireStatement2.getName(), objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
				//check if the argument lists are identical after replacements
				if(objectCreation1.identicalName(creationCoveringTheEntireStatement2)) {
					if(objectCreation1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
							s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).equals(s2.substring(s2.indexOf("[")+1, s2.lastIndexOf("]"))) &&
							s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).length() > 0) {
						return replacementInfo.getReplacements();
					}
					if(!objectCreation1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
							s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).equals(s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"))) &&
							s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).length() > 0) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && (r = creationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String creation1 : creations1) {
			AbstractCall objectCreation1 = creationMap1.get(creation1);
			if(statement1.getString().endsWith(creation1 + ";\n") && (r = objectCreation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		return null;
	}

	private boolean containsValidOperatorReplacements(ReplacementInfo replacementInfo) {
		List<Replacement> operatorReplacements = replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR);
		for(Replacement replacement : operatorReplacements) {
			if(replacement.getBefore().equals("==") && !replacement.getAfter().equals("!="))
				return false;
			if(replacement.getBefore().equals("!=") && !replacement.getAfter().equals("=="))
				return false;
			if(replacement.getBefore().equals("&&") && !replacement.getAfter().equals("||"))
				return false;
			if(replacement.getBefore().equals("||") && !replacement.getAfter().equals("&&"))
				return false;
		}
		return true;
	}

	private boolean commonConditional(String s1, String s2) {
		if((s1.contains("||") || s1.contains("&&")) && (s2.contains("||") || s2.contains("&&"))) {
			String conditional1 = prepareConditional(s1);
			String conditional2 = prepareConditional(s2);
			String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
			String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
			List<String> subConditionsAsList1 = new ArrayList<String>();
			for(String s : subConditions1) {
				subConditionsAsList1.add(s.trim());
			}
			List<String> subConditionsAsList2 = new ArrayList<String>();
			for(String s : subConditions2) {
				subConditionsAsList2.add(s.trim());
			}
			Set<String> intersection = new LinkedHashSet<String>(subConditionsAsList1);
			intersection.retainAll(subConditionsAsList2);
			if(!intersection.isEmpty()) {
				return true;
			}
			for(String subCondition1 : subConditionsAsList1) {
				for(String subCondition2 : subConditionsAsList2) {
					if(subCondition1.equals("!" + subCondition2))
						return true;
					if(subCondition2.equals("!" + subCondition1))
						return true;
				}
			}
		}
		return false;
	}

	private String prepareConditional(String s) {
		String conditional = s;
		if(s.startsWith("if(") && s.endsWith(")")) {
			conditional = s.substring(3, s.length()-1);
		}
		if(s.startsWith("while(") && s.endsWith(")")) {
			conditional = s.substring(6, s.length()-1);
		}
		if(s.startsWith("return ") && s.endsWith(";\n")) {
			conditional = s.substring(7, s.length()-2);
		}
		int indexOfEquals = s.indexOf("=");
		if(indexOfEquals > -1 && s.charAt(indexOfEquals+1) != '=' && s.endsWith(";\n")) {
			conditional = s.substring(indexOfEquals+1, s.length()-2);
		}
		return conditional;
	}

	private void replaceVariablesWithArguments(Set<String> variables, Map<String, String> parameterToArgumentMap) {
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(variables.contains(parameter)) {
				variables.add(argument);
				if(argument.contains("(") && argument.contains(")")) {
					int indexOfOpeningParenthesis = argument.indexOf("(");
					int indexOfClosingParenthesis = argument.lastIndexOf(")");
					String arguments = argument.substring(indexOfOpeningParenthesis+1, indexOfClosingParenthesis);
					if(!arguments.isEmpty() && !arguments.contains(",") && !arguments.contains("(") && !arguments.contains(")")) {
						variables.add(arguments);
					}
				}
			}
		}
	}

	private void replaceVariablesWithArguments(Map<String, AbstractCall> callMap,
			Set<String> calls, Map<String, String> parameterToArgumentMap) {
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(!parameter.equals(argument)) {
				Set<String> toBeAdded = new LinkedHashSet<String>();
				for(String call : calls) {
					String afterReplacement = ReplacementUtil.performReplacement(call, parameter, argument);
					if(!call.equals(afterReplacement)) {
						toBeAdded.add(afterReplacement);
						AbstractCall oldCall = callMap.get(call);
						AbstractCall newCall = oldCall.update(parameter, argument);
						callMap.put(afterReplacement, newCall);
					}
				}
				calls.addAll(toBeAdded);
			}
		}
	}

	private void findReplacements(Set<String> strings1, Set<String> strings2, ReplacementInfo replacementInfo, ReplacementType type) {
		for(String s1 : strings1) {
			TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
			int minDistance = replacementInfo.getRawDistance();
			for(String s2 : strings2) {
				String temp = replacementInfo.getArgumentizedString1().replaceAll(Pattern.quote(s1), Matcher.quoteReplacement(s2));
				int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
				if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
					minDistance = distanceRaw;
					Replacement replacement = new Replacement(s1, s2, type);
					double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
					replacementMap.put(distancenormalized, replacement);
					if(distanceRaw == 0) {
						break;
					}
				}
			}
			if(!replacementMap.isEmpty()) {
				Replacement replacement = replacementMap.firstEntry().getValue();
				replacementInfo.addReplacement(replacement);
				replacementInfo.setArgumentizedString1(replacementInfo.getArgumentizedString1().replaceAll(Pattern.quote(replacement.getBefore()), Matcher.quoteReplacement(replacement.getAfter())));
				if(replacementMap.firstEntry().getKey() == 0) {
					break;
				}
			}
		}
	}

	private Replacement variableReplacementWithinMethodInvocations(String s1, String s2, Set<String> variables1, Set<String> variables2) {
		for(String variable1 : variables1) {
			if(s1.contains(variable1) && !s1.equals(variable1)) {
				int startIndex1 = s1.indexOf(variable1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + variable1.length(), s1.length());
				for(String variable2 : variables2) {
					if(s2.contains(variable2) && !s2.equals(variable2)) {
						int startIndex2 = s2.indexOf(variable2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + variable2.length(), s2.length());
						if(substringBeforeIndex1.equals(substringBeforeIndex2) && substringAfterIndex1.equals(substringAfterIndex2)) {
							return new Replacement(variable1, variable2, ReplacementType.VARIABLE_NAME);
						}
					}
				}
			}
		}
		return null;
	}

	private boolean containsMethodInvocationReplacement(Set<Replacement> replacements) {
		for(Replacement replacement : replacements) {
			if(replacement instanceof MethodInvocationReplacement) {
				return true;
			}
		}
		return false;
	}

	private boolean variablesStartWithSameCase(String s1, String s2, Map<String, String> parameterToArgumentMap) {
		if(parameterToArgumentMap.values().contains(s2)) {
			return true;
		}
		if(s1.length() > 0 && s2.length() > 0) {
			if(Character.isUpperCase(s1.charAt(0)) && Character.isUpperCase(s2.charAt(0)))
				return true;
			if(Character.isLowerCase(s1.charAt(0)) && Character.isLowerCase(s2.charAt(0)))
				return true;
			if(s1.charAt(0) == '_' && s2.charAt(0) == '_')
				return true;
		}
		return false;
	}

	public boolean isEmpty() {
		return getNonMappedLeavesT1().isEmpty() && getNonMappedInnerNodesT1().isEmpty() &&
				getNonMappedLeavesT2().isEmpty() && getNonMappedInnerNodesT2().isEmpty();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLOperationBodyMapper) {
    		UMLOperationBodyMapper other = (UMLOperationBodyMapper)o;
    		return this.operation1.equals(other.operation1) && this.operation2.equals(other.operation2);
    	}
    	return false;
	}

	@Override
	public int compareTo(UMLOperationBodyMapper operationBodyMapper) {
		int thisCallChainIntersectionSum = 0;
		for(AbstractCodeMapping mapping : this.mappings) {
			if(mapping instanceof LeafMapping) {
				thisCallChainIntersectionSum += ((LeafMapping)mapping).callChainIntersection().size();
			}
		}
		int otherCallChainIntersectionSum = 0;
		for(AbstractCodeMapping mapping : operationBodyMapper.mappings) {
			if(mapping instanceof LeafMapping) {
				otherCallChainIntersectionSum += ((LeafMapping)mapping).callChainIntersection().size();
			}
		}
		if(thisCallChainIntersectionSum != otherCallChainIntersectionSum) {
			return -Integer.compare(thisCallChainIntersectionSum, otherCallChainIntersectionSum);
		}
		int thisMappings = this.mappingsWithoutBlocks();
		int otherMappings = operationBodyMapper.mappingsWithoutBlocks();
		if(thisMappings != otherMappings) {
			return -Integer.compare(thisMappings, otherMappings);
		}
		else {
			int thisExactMatches = this.exactMatches();
			int otherExactMateches = operationBodyMapper.exactMatches();
			if(thisExactMatches != otherExactMateches) {
				return -Integer.compare(thisExactMatches, otherExactMateches);
			}
			else {
				int thisEditDistance = this.editDistance();
				int otherEditDistance = operationBodyMapper.editDistance();
				if(thisEditDistance != otherEditDistance) {
					return Integer.compare(thisEditDistance, otherEditDistance);
				}
				else {
					int thisOperationNameEditDistance = this.operationNameEditDistance();
					int otherOperationNameEditDistance = operationBodyMapper.operationNameEditDistance();
					return Integer.compare(thisOperationNameEditDistance, otherOperationNameEditDistance);
				}
			}
		}
	}

	private Set<RenameVariableRefactoring> findConsistentVariableRenames() {
		Map<Replacement, Integer> replacementOccurrenceMap = getReplacementOccurrenceMap(ReplacementType.VARIABLE_NAME);
		Set<Replacement> renames = replacementOccurrenceMap.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, renames);
		allConsistentRenames.removeAll(allInconsistentRenames);
		Set<Replacement> finalConsistentRenames = new LinkedHashSet<Replacement>();
		for(Replacement replacement : allConsistentRenames) {
			if(replacementOccurrenceMap.get(replacement) > 1 || potentialParameterRename(replacement) ||
					getVariableDeclaration1(replacement) == null || getVariableDeclaration2(replacement) == null) {
				finalConsistentRenames.add(replacement);
			}
		}
		Set<RenameVariableRefactoring> variableRenames = new LinkedHashSet<RenameVariableRefactoring>();
		for(Replacement replacement : finalConsistentRenames) {
			VariableDeclaration v1 = getVariableDeclaration1(replacement);
			VariableDeclaration v2 = getVariableDeclaration2(replacement);
			if(v1 != null && v2 != null) {
				RenameVariableRefactoring ref = new RenameVariableRefactoring(v1, v2, operation1, operation2);
				if(!existsConflictingExtractVariableRefactoring(ref)) {
					variableRenames.add(ref);
				}
			}
			else {
				CandidateAttributeRename candidate = new CandidateAttributeRename(
						replacement.getBefore(), replacement.getAfter(), operation1, operation2);
				this.candidateAttributeRenames.add(candidate);
			}
		}
		return variableRenames;
	}

	private VariableDeclaration getVariableDeclaration1(Replacement replacement) {
		VariableDeclaration v1 = null;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getReplacements().contains(replacement)) {
				v1 = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				break;
			}
		}
		if(v1 == null) {
			for(UMLParameter parameter : operation1.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getBefore())) {
					v1 = vd;
					break;
				}
			}
		}
		return v1;
	}

	private VariableDeclaration getVariableDeclaration2(Replacement replacement) {
		VariableDeclaration v2 = null;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getReplacements().contains(replacement)) {
				v2 = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
			}
		}
		if(v2 == null) {
			for(UMLParameter parameter : operation2.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
					v2 = vd;
					break;
				}
			}
		}
		return v2;
	}

	private boolean existsConflictingExtractVariableRefactoring(RenameVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRef = (ExtractVariableRefactoring)refactoring;
				if(extractVariableRef.getVariableDeclaration().equals(ref.getRenamedVariable()) &&
						extractVariableRef.getOperation().equals(ref.getOperationAfter())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean potentialParameterRename(Replacement replacement) {
		int index1 = operation1.getParameterNameList().indexOf(replacement.getBefore());
		int index2 = operation2.getParameterNameList().indexOf(replacement.getAfter());
		return index1 >= 0 && index1 == index2;
	}
}
