package com.example.contextextractortu.model

data class TargetMethod(
    val className: String,
    val methodName: String,
    val sourceCode: String,
    val parameters: List<VariableInfo>,
    val methodCalls: List<MethodCall>,
    val sideEffects: List<SideEffect> = emptyList()
)

data class ClassContext(
    val name: String,
    val qualifiedName: String? = null,
    val fields: List<VariableInfo>,
    val constructors: List<ConstructorInfo> = emptyList(),
    val methods: List<MethodSignature>
)

data class MethodCall(
    val className: String,
    val methodName: String
)

data class MethodSignature(
    val name: String,
    val parameters: List<String>,
    val returnType: String = "void",
    val isAbstract: Boolean = false,
    val isFinal: Boolean = false,
    val isStatic: Boolean = false
)

data class VariableInfo(
    val name: String,
    val type: String
)

data class ConstructorInfo(
    val name: String,
    val parameters: List<VariableInfo>,
    val isAutowired: Boolean = false
)

data class SideEffect(
    val fieldName: String,
    val operation: String
)