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
                addToMapByType(it.variableName, it.isNullable ,it.type)
                addComma()
                addNewLine()
            }
        }
        addSemicolon()
    }
}

private fun Template.addToMapByType(variableName: String, nullable: Boolean, varType: String) {
    if (varType == "String" || varType == "String?"
        || varType == "int" || varType == "int?"
        || varType == "double" || varType == "double?"
        || varType == "bool" || varType == "bool?"
        || varType == "num" || varType == "num?"
        || varType == "dynamic"
    ) {
        addTextSegment(variableName)
    } else if (varType.contains("List")) {
        val start = varType.indexOfFirst { c -> c == '<' } + 1
        val end = varType.indexOfLast { c -> c == '>' }
        val childType = varType.substring(start, end)
        addTextSegment(variableName)
        if (nullable) {
            addTextSegment("?")
        }
        addTextSegment(".map")
        withParentheses {
            addTextSegment("(e) => ")
            addToMapByType(variableName = "e", nullable = childType.lastOrNull() == '?', varType = childType)
        }
        addTextSegment(".toList()")
    } else if (varType.contains("Map")) {
        val start = varType.indexOfFirst { c -> c == ',' } + 2
        val end = varType.indexOfLast { c -> c == '>' }
        val childType = varType.substring(start, end)
        addTextSegment(variableName)
        if (nullable) {
            addTextSegment("?")
        }
        addTextSegment(".map")
        withParentheses {
            addTextSegment("(key,value) => MapEntry")
            withParentheses {
                addTextSegment("key")
                addComma()
                addToMapByType(variableName = "value", nullable = childType.lastOrNull() == '?', varType = childType)
            }
        }
    } else {
        addTextSegment(variableName)
        if (nullable) {
            addTextSegment("?")
        }
        addTextSegment(".toMap()")
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
                if (varType == "int"
                    || varType == "double"
                    || varType == "num"
                    || varType == "String"
                    || varType == "bool"
                    || varType == "dynamic"
                ) {
                    addTempleteForBasicType(it, addKeyMapper, noImplicitCasts)
                } else if (varType.contains("List")) {
                    addTempleteForListType(it, varType)
                } else if (varType.contains("Map")) {
                    addTempleteForMapType(it, varType)
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
    if (it.isNullable) {
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
    }
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
        addTextSegment("List<dynamic>")
        if (it.isNullable) {
            addTextSegment("?")
        }
    }
    addNewLine()
    if (it.isNullable) {
        addTextSegment("?")
    }
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
            || childType == "bool" || childType == "bool?"
            || childType == "num" || childType == "num?"
        ) {
            addTextSegment("e")
            addSpace()
            addTextSegment("as")
            addSpace()
            addTextSegment(childType)
        } else {
            val childTypeNullable = childType.lastOrNull() == '?'
            if (childTypeNullable) {
                addTextSegment("e == null ? null :")
            }
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

private fun Template.addTempleteForMapType(
    it: AliasedVariableTemplateParam,
    varType: String
) {
    val start = varType.indexOfFirst { c -> c == ',' } + 2  // dont get space Map<String, Model>
    val end = varType.indexOfLast { c -> c == '>' }
    val childType = varType.substring(start, end)

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
        addTextSegment("Map<String, dynamic>")
        if (it.isNullable) {
            addTextSegment("?")
        }
    }
    addNewLine()
    if (it.isNullable) {
        addTextSegment("?")
    }
    addTextSegment(".map")
    generateChildTempleteOfMapType(childType)
    addComma()
    addNewLine()
}

private fun Template.generateChildTempleteOfMapType(childType: String) {
    val childTypeNullable = childType.lastOrNull() == '?'
    withParentheses {
        addTextSegment("(key, value)")
        addSpace()
        addTextSegment("=>")
        addSpace()
        addTextSegment("MapEntry")
        withParentheses {
            addTextSegment("key")
            addComma()
            addSpace()
            if (childType == "String" || childType == "String?"
                || childType == "int" || childType == "int?"
                || childType == "double" || childType == "double?"
                || childType == "bool" || childType == "bool?"
                || childType == "num" || childType == "num?"
            ) {
                addTextSegment("value")
                addSpace()
                addTextSegment("as")
                addSpace()
                addTextSegment(childType)
            } else if (childType.contains("Map")) {
                withParentheses {
                    addTextSegment("value as Map<String, dynamic>")
                    if (childTypeNullable) {
                        addTextSegment("?")
                    }
                }
                if (childTypeNullable) {
                    addTextSegment("?")
                }
                addTextSegment(".map")
                val start = childType.indexOfFirst { c -> c == ',' } + 2  // dont get space Map<String, Model>
                val end = childType.indexOfLast { c -> c == '>' }
                val subChildType = childType.substring(start, end)
                generateChildTempleteOfMapType(subChildType)
            } else if (childType.contains("List")) {
                withParentheses {
                    addTextSegment("(e)")
                    addSpace()
                    addTextSegment("=>")
                    addSpace()
                    val start = childType.indexOfFirst { c -> c == '<' } + 1  // dont get space Map<String, Model>
                    val end = childType.indexOfLast { c -> c == '>' }
                    val subChildType = childType.substring(start, end)
                    if (subChildType.lastOrNull() == '?') {
                        addTextSegment("e == null ? null :")
                    }
                    addTextSegment(childType)
                    addTextSegment(".")
                    addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
                    withParentheses {
                        addTextSegment("e as Map<String, dynamic>")
                    }
                }
            } else {
                if (childTypeNullable) {
                    addTextSegment("value == null ? null :")
                }
                addTextSegment(childType)
                addTextSegment(".")
                addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
                withParentheses {
                    addTextSegment("value as Map<String, dynamic>")
                }
            }
        }
    }
}
