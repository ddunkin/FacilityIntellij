package io.github.facilityapi.intellij.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import io.github.facilityapi.intellij.FsdBundle
import io.github.facilityapi.intellij.psi.FsdDecoratedElement
import io.github.facilityapi.intellij.psi.FsdTypes
import io.github.facilityapi.intellij.reference.createFromText

class CombineAttributeListsIntention : PsiElementBaseIntentionAction() {
    override fun startInWriteAction(): Boolean = true

    override fun getText() = TEXT

    override fun getFamilyName() = FAMILY_NAME

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val decoratedElement = PsiTreeUtil.getParentOfType(element, FsdDecoratedElement::class.java) ?: return false
        return decoratedElement.attributeListList.size > 1
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return

        val decoratedElement = PsiTreeUtil.getParentOfType(element, FsdDecoratedElement::class.java) ?: return
        val attributeLists = decoratedElement.attributeListList
        val targetAttributeList = attributeLists.first()
        val attributeListsToDiscard = attributeLists.drop(1)
        val attributesToFold = attributeListsToDiscard.flatMap { it.attributeList }

        val psiFacade = PsiParserFacade.SERVICE.getInstance(project)
        val space = psiFacade.createWhiteSpaceFromText(" ")

        val comma = createFromText(project, "[first, second] service Dummy { }")
            .first { it.elementType == FsdTypes.COMMA }

        for (attribute in attributesToFold) {
            val addedComma = targetAttributeList.addAfter(comma, targetAttributeList.attributeList.last())
            val addedAttribute = targetAttributeList.addAfter(attribute, addedComma)
            targetAttributeList.addBefore(space, addedAttribute)
        }

        for (attributeList in attributeListsToDiscard) {
            attributeList.delete()
        }
    }

    companion object {
        val TEXT = FsdBundle.getMessage("intentions.attribute.lists.combine.text")
        val FAMILY_NAME = FsdBundle.getMessage("intentions.attribute.lists.combine.family")
    }
}
