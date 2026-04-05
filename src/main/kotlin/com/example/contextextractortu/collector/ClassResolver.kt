package com.example.contextextractortu.collector

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

class ClassResolver(private val project: Project) {

    fun resolveClass(fqn: String): PsiClass? {
        val facade = JavaPsiFacade.getInstance(project)
        // GlobalSearchScope.projectScope cherche dans tout le code source du projet
        return facade.findClass(fqn, GlobalSearchScope.projectScope(project))
    }

    // Utile pour résoudre un type à partir d'un PsiType (ex: type d'un champ)
    fun resolveFromType(type: PsiType): PsiClass? {
        return (type as? PsiClassType)?.resolve()
    }
}