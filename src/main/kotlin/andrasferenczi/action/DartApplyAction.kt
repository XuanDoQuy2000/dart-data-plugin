package andrasferenczi.action

import andrasferenczi.action.data.PerformAction
import andrasferenczi.action.init.ActionData
import andrasferenczi.action.utils.createCopyWithDeleteCall
import andrasferenczi.action.utils.selectFieldsWithDialog
import andrasferenczi.configuration.ConfigurationDataManager
import andrasferenczi.declaration.allMembersFinal
import andrasferenczi.declaration.fullTypeName
import andrasferenczi.declaration.isNullable
import andrasferenczi.declaration.variableName
import andrasferenczi.ext.psi.extractClassName
import andrasferenczi.ext.psi.findMethodsByName
import andrasferenczi.templater.*
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.DartClassDefinition

class DartApplyAction : BaseAnAction() {
    override fun processAction(
        event: AnActionEvent,
        actionData: ActionData,
        dartClass: DartClassDefinition
    ): PerformAction? {
        val declarations = selectFieldsWithDialog(actionData.project, dartClass) ?: return null
        val (project, _, _, _) = actionData

        val variableNames: List<AliasedVariableTemplateParam> = declarations
            .map {
                AliasedVariableTemplateParamImpl(
                    variableName = it.variableName,
                    type = it.fullTypeName
                        ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor"),
                    publicVariableName = it.publicVariableName,
                    isNullable = it.isNullable
                )
            }

        val templateManager = TemplateManager.getInstance(project)
        val dartClassName = dartClass.extractClassName()

        val template = createApplyTemplate(
            templateManager,
            ApplyTemplateParams(
                className = dartClassName,
                variables = variableNames
            )
        )

        return PerformAction(
            createDeleteCall(dartClass),
            template = template
        )
    }

    private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
        val toString = dartClass.findMethodsByName(TemplateConstants.APPLY_METHOD_NAME)
            .firstOrNull()
            ?: return null

        return { toString.delete() }
    }

}