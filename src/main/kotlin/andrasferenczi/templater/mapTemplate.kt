package andrasferenczi.templater

import andrasferenczi.ext.*
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager

data class MapTemplateParams(
    val className: String,
    val variables: List<AliasedVariableTemplateParam>,
    val useNewKeyword: Boolean,
    val addKeyMapper: Boolean,
    val noImplicitCasts: Boolean
)

// The 2 will be generated with the same function
fun createMapTemplate(
    templateManager: TemplateManager,
    params: MapTemplateParams
): Template {

    return templateManager.createTemplate(
        TemplateType.MapTemplate.templateKey,
        TemplateConstants.DART_TEMPLATE_GROUP
    ).apply {
        addToMap(params)
        addNewLine()
        addNewLine()
        addFromMap(params)
    }
}

private fun Template.addAssignKeyMapperIfNotValid() {
    addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
    addSpace()
    addTextSegment("??=")
    addSpace()
    withParentheses {
        addTextSegment(TemplateConstants.KEY_VARIABLE_NAME)
    }
    addSpace()
    addTextSegment("=>")
    addSpace()
    addTextSegment(TemplateConstants.KEY_VARIABLE_NAME)
    addSemicolon()
    addNewLine()
    addNewLine()
}

private fun Template.addToMap(params: MapTemplateParams) {
    val (_, variables, _, addKeyMapper, _) = params

    isToReformat = true

    addTextSegment("Map<String, dynamic>")
    addSpace()
    addTextSegment(TemplateConstants.TO_MAP_METHOD_NAME)
    withParentheses {
        if (addKeyMapper) {
            withCurlyBraces {
                addNewLine()
                addTextSegment("String Function(String key)? ${TemplateConstants.KEYMAPPER_VARIABLE_NAME}")
                addComma()
                addNewLine()
            }
        }
    }
    addSpace()
    withCurlyBraces {

        if (addKeyMapper) {
            addAssignKeyMapperIfNotValid()
        }

        addTextSegment("return")
        addSpace()
        withCurlyBraces {
            addNewLine()

            variables.forEach {
                "'${it.mapKeyString}'".also { keyParam ->
                    if (addKeyMapper) {
                        addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                        withParentheses {
                            addTextSegment(keyParam)
                        }
                    } else {
                        addTextSegment(keyParam)
                    }
                }

                addTextSegment(":")
                addSpace()
                addTextSegment(it.variableName)
                addComma()
                addNewLine()
            }
        }
        addSemicolon()
    }
}

private fun Template.addFromMap(
    params: MapTemplateParams
) {
    val (className, variables, useNewKeyword, addKeyMapper, noImplicitCasts) = params

    isToReformat = true

    addTextSegment("factory")
    addSpace()
    addTextSegment(className)
    addTextSegment(".")
    addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
    withParentheses {
        if (addKeyMapper) {
            addNewLine()
            // New line does not format, no matter what is in this if statement
            addSpace()
        }
        addTextSegment("Map<String, dynamic>")
        addSpace()
        addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)

        if (addKeyMapper) {
            addComma()
            addSpace()
            withCurlyBraces {
                addNewLine()
                addTextSegment("String Function(String ${TemplateConstants.KEY_VARIABLE_NAME})?")
                addSpace()
                addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                addComma()
                addNewLine()
            }
        }
    }
    addSpace()
    withCurlyBraces {

        if (addKeyMapper) {
            addAssignKeyMapperIfNotValid()
        }

        addTextSegment("return")
        addSpace()
        if (useNewKeyword) {
            addTextSegment("new")
            addSpace()
        }
        addTextSegment(className)
        withParentheses {
            addNewLine()
            variables.forEach { it ->
                val varType = it.type
                if (varType == "int" || varType == "double" || varType == "String" || varType == "bool" || varType == "DateTime") {
                    addTempleteForBasicType(it, addKeyMapper, noImplicitCasts)
                } else if (varType.contains("List")) {
                    addTempleteForListType(it, varType)
                } else {
                    addTempleteForObjectType(it)
                }
            }
        }
        addSemicolon()
    }
}

private fun Template.addTempleteForObjectType(it: AliasedVariableTemplateParam) {
    val varType = it.type
    addTextSegment(it.publicVariableName)
    addTextSegment(":")
    addSpace()

    addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)

    withBrackets {
        "'${it.mapKeyString}'".also { keyParam ->
            addTextSegment(keyParam)
        }
    }
    addSpace()
    addTextSegment("==")
    addSpace()
    addTextSegment("null")
    addNewLine()
    addTextSegment("? null")
    addNewLine()
    addTextSegment(": ")
    addTextSegment(varType)
    addTextSegment(".")
    addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
    withParentheses {
        addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)
        withBrackets {
            "'${it.mapKeyString}'".also { keyParam ->
                addTextSegment(keyParam)
            }
        }
        addSpace()
        addTextSegment("as Map<String, dynamic>")
    }
    addComma()
}

private fun Template.addTempleteForListType(
    it: AliasedVariableTemplateParam,
    varType: String
) {
    addTextSegment(it.publicVariableName)
    addTextSegment(":")
    addSpace()
    withParentheses {
        addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)

        withBrackets {
            "'${it.mapKeyString}'".also { keyParam ->
                addTextSegment(keyParam)
            }
        }
        addSpace()
        addTextSegment("as")
        addSpace()
        addTextSegment("List<dynamic>?")
    }
    addNewLine()
    addTextSegment("?")
    addTextSegment(".map")
    withParentheses {
        addTextSegment("(e)")
        addSpace()
        addTextSegment("=>")
        addSpace()
        val start = varType.indexOfFirst { c -> c == '<' } + 1
        val end = varType.indexOfFirst { c -> c == '>' }
        val childType = varType.substring(start, end)
        if (childType == "String" || childType == "String?"
            || childType == "int" || childType == "int?"
            || childType == "double" || childType == "double?"
            || childType == "bool" || childType == "bool?") {
            addTextSegment("e")
            addSpace()
            addTextSegment("as")
            addSpace()
            addTextSegment(childType)
        }else {
            addTextSegment(varType.substring(start, end))
            addTextSegment(".")
            addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
            withParentheses {
                addTextSegment("e as Map<String, dynamic>")
            }
        }
    }
    addTextSegment(".toList()")
    addComma()
    addNewLine()
}

private fun Template.addTempleteForBasicType(
    it: AliasedVariableTemplateParam,
    addKeyMapper: Boolean,
    noImplicitCasts: Boolean
) {
    addTextSegment(it.publicVariableName)
    addTextSegment(":")
    addSpace()
    addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)

    withBrackets {
        "'${it.mapKeyString}'".also { keyParam ->
            if (addKeyMapper) {
                addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                withParentheses {
                    addTextSegment(keyParam)
                }
            } else {
                addTextSegment(keyParam)
            }
        }
    }

    if (noImplicitCasts) {
        addSpace()
        addTextSegment("as")
        addSpace()
        addTextSegment(it.type)
        if (it.isNullable) {
            addTextSegment("?")
        }
    }

    addComma()
    addNewLine()
}
