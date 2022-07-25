package andrasferenczi.templater

import andrasferenczi.ext.*
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager

data class ApplyTemplateParams(
    val className: String,
    val variables: List<AliasedVariableTemplateParam>
)

fun createApplyTemplate(
    templateManager: TemplateManager,
    params: ApplyTemplateParams
): Template {
    val variables = params.variables
    val className = params.className
    return templateManager.createDartTemplate(TemplateType.ApplyMethod).apply {
        isToReformat = true
        addTextSegment(className)
        addSpace()
        addTextSegment(TemplateConstants.APPLY_METHOD_NAME)
        withParentheses {
            if (variables.isNotEmpty()) {
                withCurlyBraces {
                    addNewLine()

                    variables.forEach {
                        addTextSegment(it.type)
                        addTextSegment("?")
                        addSpace()
                        addTextSegment(it.publicVariableName)
                        addComma()
                        addNewLine()
                    }
                }
            }
        }
        withCurlyBraces {
            addNewLine()

            variables.forEach { variable ->
                addTextSegment("if")
                addSpace()
                withParentheses {
                    addTextSegment(variable.publicVariableName)
                    addSpace()
                    addTextSegment("!=")
                    addSpace()
                    addTextSegment("null")
                }
                addSpace()
                addTextSegment("this")
                addDot()
                addTextSegment(variable.publicVariableName)
                addSpace()
                addTextSegment("=")
                addSpace()
                addTextSegment(variable.publicVariableName)
                addSemicolon()
                addNewLine()
            }
            addTextSegment("return this;")
        }
    }
}