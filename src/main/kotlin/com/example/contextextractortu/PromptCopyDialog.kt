package com.example.contextextractortu

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.labels.LinkLabel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

/**
 * Dialog pour afficher et copier le prompt généré
 */
class PromptCopyDialog(
    project: com.intellij.openapi.project.Project,
    private val prompt: String
) : DialogWrapper(project, true) {

    private lateinit var textArea: JBTextArea
    private lateinit var copyButton: JButton
    private lateinit var copyToFileButton: JButton

    init {
        title = "Contexte IA Généré"
        init()
    }

    override fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(800, 600)

        // Zone de texte avec le prompt (lecture seule)
        textArea = JBTextArea(prompt).apply {
            isEditable = false
            font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
            lineWrap = false
            wrapStyleWord = false
        }

        val scrollPane = JBScrollPane(textArea)
        scrollPane.preferredSize = Dimension(780, 500)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Panel des boutons
        val buttonPanel = JPanel(BorderLayout())

        copyButton = JButton("Copier le prompt").apply {
            addActionListener { copyToClipboard() }
        }
        buttonPanel.add(copyButton, BorderLayout.WEST)

        copyToFileButton = JButton("Sauvegarder dans un fichier...").apply {
            addActionListener { copyToFile() }
        }
        buttonPanel.add(copyToFileButton, BorderLayout.CENTER)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun copyToClipboard() {
        try {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val transferable = java.awt.datatransfer.StringSelection(prompt)
            clipboard.setContents(transferable, null)
            Messages.showInfoMessage("Prompt copié dans le presse-papier !", "Copie réussie")
        } catch (e: Exception) {
            Messages.showErrorDialog("Échec de la copie : ${e.message}", "Erreur")
        }
    }

    private fun copyToFile() {
        try {
            val tempFile = createTempFile("context-prompt", ".md")
            tempFile.writeText(prompt)
            Messages.showInfoMessage("Prompt sauvegardé dans : ${tempFile.toAbsolutePath()}", "Fichier créé")
        } catch (e: Exception) {
            Messages.showErrorDialog("Échec de la sauvegarde : ${e.message}", "Erreur")
        }
    }

    override fun getPreferredFocusedComponent(): javax.swing.JComponent? {
        return textArea
    }
}
