package com.example.contextextractortu.model

data class TargetMethod(
    val className: String,
    val methodName: String,
    val sourceCode: String,
    val parameters: List<VariableInfo>,
    val methodCalls: List<MethodCall>
)

data class ClassContext(
    val name: String,
    val fields: List<VariableInfo>,
    val methods: List<MethodSignature>
)

data class MethodCall(
    val className: String,
    val methodName: String
)

data class MethodSignature(
    val name: String,
    val parameters: List<String>
)

data class VariableInfo(
    val name: String,
    val type: String
)