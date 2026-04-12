package com.example.contextextractortu.collector
// API IntelliJ pour l'analyse de code Java
import com.example.contextextractortu.model.*
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression

class PsiScanner {

    fun extractTargetMethod(method: PsiMethod): TargetMethod {
        val calls = mutableListOf<MethodCall>()

        // Extraction des appels de méthodes (CallGraphBuilder simplifié)
        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolved = expression.resolveMethod()
                val className = resolved?.containingClass?.name ?: "Unknown"
                calls.add(MethodCall(className, expression.methodExpression.referenceName ?: ""))
            }
        })

        return TargetMethod(
            className = method.containingClass?.name ?: "Unknown",
            methodName = method.name,
            sourceCode = method.text,
            parameters = method.parameterList.parameters.map {
                VariableInfo(it.name, it.type.presentableText)
            },
            methodCalls = calls
        )
    }

    fun extractClassContext(psiClass: PsiClass): ClassContext {
        return ClassContext(
            name = psiClass.name ?: "Unknown",
            fields = psiClass.fields.map { VariableInfo(it.name, it.type.presentableText) },
            methods = psiClass.methods.map { m ->
                MethodSignature(m.name, m.parameterList.parameters.map { it.type.presentableText })
            }
        )
    }
}