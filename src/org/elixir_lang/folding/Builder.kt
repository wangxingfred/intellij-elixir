package org.elixir_lang.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.util.PsiTreeUtil
import org.elixir_lang.psi.*
import org.elixir_lang.psi.call.Call
import org.elixir_lang.psi.call.name.Function
import org.elixir_lang.psi.call.name.Module
import org.elixir_lang.psi.impl.ElixirPsiImplUtil
import org.elixir_lang.psi.impl.call.finalArguments
import org.elixir_lang.psi.impl.siblingExpression
import org.elixir_lang.psi.operation.Type
import org.elixir_lang.psi.operation.infix.Normalized
import org.elixir_lang.reference.ModuleAttribute.Companion.isDocumentationName
import org.elixir_lang.reference.ModuleAttribute.Companion.isTypeName
import java.util.*

class Builder : FoldingBuilderEx() {
    /**
     * Builds the folding regions for the specified node in the AST tree and its children.
     *
     * @param root     the element for which folding is requested.
     * @param document the document for which folding is built. Can be used to retrieve line
     * numbers for folding regions.
     * @param quick    whether the result should be provided as soon as possible. Is true, when
     * an editor is opened and we need to auto-fold something immediately, like Java imports.
     * If true, one should perform no reference resolving and avoid complex checks if possible.
     * @return the array of folding descriptors.
     */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val foldingDescriptorList: MutableList<FoldingDescriptor> = ArrayList()
        PsiTreeUtil.processElements(root,
                object : PsiElementProcessor<PsiElement> {
                    private val foldingGroupByModuleAttributeName: MutableMap<String, FoldingGroup> = HashMap()

                    override fun execute(element: PsiElement): Boolean =
                            when (element) {
                                is AtNonNumericOperation -> {
                                    execute(element)
                                }
                                is AtUnqualifiedNoParenthesesCall<*> -> {
                                    execute(element)
                                }
                                is ElixirDoBlock -> {
                                    execute(element)
                                }
                                is ElixirStabOperation -> {
                                    execute(element)
                                }
                                is Call -> {
                                    execute(element)
                                }
                                else -> {
                                    true
                                }
                            }

                    /*
                     * Private Instance Methods
                     */

                    private fun execute(atNonNumericOperation: AtNonNumericOperation): Boolean =
                            if (!quick) {
                                slowExecute(atNonNumericOperation)
                            } else {
                                true
                            }

                    private fun execute(atUnqualifiedNoParenthesesCall: AtUnqualifiedNoParenthesesCall<*>): Boolean {
                        val moduleAttributeName = ElixirPsiImplUtil.moduleAttributeName(atUnqualifiedNoParenthesesCall)
                        val name = moduleAttributeName.substring(1)
                        if (isDocumentationName(name)) {
                            val noParenthesesOneArgument = atUnqualifiedNoParenthesesCall.noParenthesesOneArgument
                            foldingDescriptorList.add(
                                    FoldingDescriptor(
                                            noParenthesesOneArgument.node,
                                            noParenthesesOneArgument.textRange,
                                            null,
                                            "\"...\""
                                    )
                            )
                        } else if (isTypeName(name)) {
                            atUnqualifiedNoParenthesesCall
                                    .noParenthesesOneArgument
                                    .children
                                    .singleOrNull()
                                    ?.let { child ->
                                        if (child is Type) {
                                            val rightOperand: PsiElement? = Normalized.rightOperand(child)
                                            if (rightOperand != null) {
                                                foldingDescriptorList.add(
                                                        FoldingDescriptor(
                                                                rightOperand.node,
                                                                rightOperand.textRange,
                                                                null,
                                                                "..."
                                                        )
                                                )
                                            }
                                        }
                                    }
                        }
                        return true
                    }

                    private fun execute(call: Call): Boolean {
                        for (resolvedFunctionName in RESOLVED_FUNCTION_NAMES) {
                            if (call.isCalling(Module.KERNEL, resolvedFunctionName)) {
                                if (isFirstInGroup(call, Module.KERNEL, resolvedFunctionName)) {
                                    call.finalArguments()?.firstOrNull()?.let { firstFinalArgument ->
                                        val last = lastInGroup(call, Module.KERNEL, resolvedFunctionName)
                                        val textRange = TextRange(
                                                firstFinalArgument.textOffset,
                                                last.textRange.endOffset
                                        )
                                        foldingDescriptorList.add(
                                                FoldingDescriptor(
                                                        call.parent.node,
                                                        textRange,
                                                        null,
                                                        "..."
                                                )
                                        )
                                    }

                                }
                            }
                        }
                        return true
                    }

                    private fun execute(doBlock: ElixirDoBlock): Boolean {
                        foldingDescriptorList.add(FoldingDescriptor(doBlock, doBlock.textRange))
                        return true
                    }

                    private fun execute(stabOperation: ElixirStabOperation): Boolean {
                        val startOffset = stabOperation.operator().textOffset
                        val endOffset = stabOperation.textRange.endOffset
                        val textRange = TextRange(startOffset, endOffset)
                        foldingDescriptorList.add(FoldingDescriptor(stabOperation, textRange))
                        return true
                    }

                    private fun isFirstInGroup(call: Call,
                                               resolvedModuleName: String,
                                               resolvedFunctionName: String): Boolean {
                        val previousSiblingExpression = ElixirPsiImplUtil.previousSiblingExpression(call)

                        return if (previousSiblingExpression is Call) {
                            !previousSiblingExpression.isCalling(resolvedModuleName, resolvedFunctionName)
                        } else {
                            true
                        }
                    }

                    private fun lastInGroup(first: Call,
                                            resolvedModuleName: String,
                                            resolvedFunctionName: String): Call {
                        var last = first
                        while (true) {
                            val nextSibling = last.siblingExpression(ElixirPsiImplUtil.NEXT_SIBLING)
                            if (nextSibling is Call && nextSibling.isCalling(resolvedModuleName, resolvedFunctionName)) {
                                last = nextSibling
                                continue
                            }
                            break
                        }
                        return last
                    }

                    private fun slowExecute(atNonNumericOperation: AtNonNumericOperation): Boolean =
                            atNonNumericOperation
                                    .reference
                                    ?.let { slowExecute(atNonNumericOperation, it) }
                                    ?: true

                    private fun slowExecute(
                            atNonNumericOperation: AtNonNumericOperation,
                            atUnqualifiedNoParenthesesCall: AtUnqualifiedNoParenthesesCall<*>
                    ): Boolean = slowExecute(
                            atNonNumericOperation,
                            atUnqualifiedNoParenthesesCall,
                            atUnqualifiedNoParenthesesCall.noParenthesesOneArgument.text
                    )

                    private fun slowExecute(atNonNumericOperation: AtNonNumericOperation,
                                            target: PsiElement): Boolean =
                            when (target) {
                                is AtUnqualifiedNoParenthesesCall<*> -> {
                                    slowExecute(
                                            atNonNumericOperation,
                                            target
                                    )
                                }
                                is QualifiableAlias -> {
                                    slowExecute(
                                            atNonNumericOperation,
                                            target
                                    )
                                }
                                else -> {
                                    true
                                }
                            }

                    private fun slowExecute(atNonNumericOperation: AtNonNumericOperation,
                                            reference: PsiReference): Boolean =
                            reference
                                    .resolve()
                                    ?.let { slowExecute(atNonNumericOperation, it) }
                                    ?: true

                    private fun slowExecute(atNonNumericOperation: AtNonNumericOperation,
                                            qualifiableAlias: QualifiableAlias): Boolean =
                            slowExecute(atNonNumericOperation, qualifiableAlias, qualifiableAlias.name)

                    private fun slowExecute(atNonNumericOperation: AtNonNumericOperation,
                                            element: PsiElement,
                                            placeHolderText: String?): Boolean {
                        val moduleAttributeName = atNonNumericOperation.moduleAttributeName()
                        val foldingGroup = foldingGroupByModuleAttributeName.computeIfAbsent(moduleAttributeName) { debugName: String? -> FoldingGroup.newGroup(debugName) }
                        foldingDescriptorList.add(
                                object : FoldingDescriptor(
                                        atNonNumericOperation.node,
                                        atNonNumericOperation.textRange,
                                        foldingGroup, setOf<Any>(element)) {
                                    override fun getPlaceholderText(): String? {
                                        return placeHolderText
                                    }
                                }
                        )
                        return true
                    }
                }
        )
        return foldingDescriptorList.toTypedArray()
    }

    /**
     * Returns the text which is displayed in the editor for the folding region related to the
     * specified node when the folding region is collapsed.
     *
     * @param node the node for which the placeholder text is requested.
     * @return the placeholder text.
     */
    override fun getPlaceholderText(node: ASTNode): String? =
            when (node.psi) {
                is ElixirDoBlock -> {
                    "do: ..."
                }
                is ElixirStabOperation -> {
                    "-> ..."
                }
                else -> {
                    null
                }
            }

    /**
     * Returns the default collapsed state for the folding region related to the specified node.
     *
     * @param node the node for which the collapsed state is requested.
     * @return true if the region is collapsed by default, false otherwise.
     */
    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        val element = node.psi
        return if (element is AtNonNumericOperation) {
            ElixirFoldingSettings.getInstance().isReplaceModuleAttributesWithValues
        } else {
            element.children.filterIsInstance<Call>().any { child ->
                RESOLVED_FUNCTION_NAMES.any { resolvedFunctionName ->
                    child.isCalling(Module.KERNEL, resolvedFunctionName) &&
                            ElixirFoldingSettings
                                    .getInstance()
                                    .isCollapseElixirModuleDirectiveGroups
                }
            }
        }
    }

    companion object {
        private val RESOLVED_FUNCTION_NAMES = arrayOf(Function.ALIAS, Function.IMPORT, Function.REQUIRE, Function.USE)
    }
}
