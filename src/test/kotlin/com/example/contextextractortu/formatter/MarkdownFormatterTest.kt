package com.example.contextextractortu.formatter
//
//import com.example.contextextractortu.model.AiContextReport
//import com.example.contextextractortu.model.ClassContext
//import com.example.contextextractortu.model.TargetMethod
//import com.example.contextextractortu.model.VariableInfo
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.Test
//
//class MarkdownFormatterTest {
//
//    @Test
//    fun `should generate markdown with class and method`() {
//        val context = AiContextReport(
//            targetMethod = TargetMethod(
//                className = "UserService",
//                methodName = "create",
//                sourceCode = "public void create() {}",
//                parameters = emptyList(),
//                methodCalls = emptyList()
//            ),
//            classUnderTest = ClassContext(
//                name = "UserService",
//                fields = listOf(
//                    VariableInfo("userRepository", "UserRepository")
//                ),
//                methods = emptyList()
//            ),
//            dependencies = emptyList()
//        )
//
//        val formatter = MarkdownFormatter(PromptTemplateEngine())
//
//        val result = formatter.generate(context)
//
//        println(result)
//
//        assertTrue(result.contains("UserService"))
//        assertTrue(result.contains("userRepository"))
//        assertTrue(result.contains("create"))
//    }
//}