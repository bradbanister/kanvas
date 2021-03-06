package me.tomassetti.kanvas

import org.fife.ui.autocomplete.*
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.*
import java.io.File
import java.nio.charset.Charset
import java.util.*
import javax.swing.*
import javax.swing.plaf.metal.MetalTabbedPaneUI
import javax.swing.plaf.synth.SynthScrollBarUI
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import javax.swing.text.Segment

private val BACKGROUND = Color(39, 40, 34)
private val BACKGROUND_SUBTLE_HIGHLIGHT = Color(49, 50, 44)
private val BACKGROUND_DARKER = Color(23, 24, 20)
private val BACKGROUND_LIGHTER = Color(109, 109, 109)

class TextPanel(textArea: RSyntaxTextArea, var file : File?) : RTextScrollPane(textArea) {
    val text : String
        get() = textArea.text
    var title : String
        get() = tabbedPane().getTitleAt(index())
        set(value) {
            tabbedPane().setTitleAt(index(), value)
        }
    private fun tabbedPane() = this.parent as MyTabbedPane
    private fun index() = tabbedPane().indexOfComponent(this)
    fun close() {
        tabbedPane().removeTabAt(index())
    }

    fun  changeLanguageSupport(languageSupport: LanguageSupport) {
        (textArea.document as RSyntaxDocument).setSyntaxStyle(AntlrTokenMaker(languageSupport.antlrLexerFactory))
        (textArea as RSyntaxTextArea).syntaxScheme = languageSupport.syntaxScheme
    }
}

private fun makeTextPanel(font: Font, languageSupport: LanguageSupport, initialContenxt: String = "", file: File? = null) : TextPanel {
    val textArea = RSyntaxTextArea(20, 60)

    (textArea.document as RSyntaxDocument).setSyntaxStyle(AntlrTokenMaker(languageSupport.antlrLexerFactory))

    textArea.syntaxScheme = languageSupport.syntaxScheme
    textArea.text = initialContenxt
    textArea.isCodeFoldingEnabled = true
    textArea.font = font
    textArea.background = BACKGROUND
    textArea.foreground = Color.WHITE
    textArea.currentLineHighlightColor = BACKGROUND_SUBTLE_HIGHLIGHT
    val textPanel = TextPanel(textArea, file)

    val provider = createCompletionProvider(languageSupport)
    val ac = AutoCompletion(provider)
    ac.install(textArea)

    textPanel.viewportBorder = BorderFactory.createEmptyBorder()
    textPanel.verticalScrollBar.ui = object : SynthScrollBarUI() {
        override fun configureScrollBarColors() {
            super.configureScrollBarColors()
        }
    }
    return textPanel
}

fun  createCompletionProvider(languageSupport: LanguageSupport): CompletionProvider {
    val cp = object : CompletionProviderBase() {
        override fun getCompletionsAt(comp: JTextComponent?, p: Point?): MutableList<Completion>? {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getParameterizedCompletions(tc: JTextComponent?): MutableList<ParameterizedCompletion>? {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private val seg = Segment()
        private val autoCompletionSuggester = AntlrAutoCompletionSuggester(languageSupport.parserData!!.ruleNames,
                languageSupport.parserData!!.vocabulary, languageSupport.parserData!!.atn)

        private fun beforeCaret(comp: JTextComponent) : String {
            val doc = comp.document

            val dot = comp.caretPosition
            val root = doc.defaultRootElement
            val index = root.getElementIndex(dot)
            val elem = root.getElement(index)
            var start = elem.startOffset
            val len = dot - start
            return doc.getText(start, len)
        }

        override fun getCompletionsImpl(comp: JTextComponent): MutableList<Completion>? {
            val retVal = ArrayList<Completion>()
            val code = beforeCaret(comp)
            autoCompletionSuggester.suggestions(EditorContextImpl(code, languageSupport.antlrLexerFactory)).forEach {
                if (it.type != -1) {
                    var proposition : String? = languageSupport.parserData!!.vocabulary.getLiteralName(it.type)
                    if (proposition != null) {
                        if (proposition.startsWith("'") && proposition.endsWith("'")) {
                            proposition = proposition.substring(1, proposition.length - 1)
                        }
                        retVal.add(BasicCompletion(this, proposition))
                    }
                }
            }

            return retVal

        }

        protected fun isValidChar(ch: Char): Boolean {
            return Character.isLetterOrDigit(ch) || ch == '_'
        }

        override fun getAlreadyEnteredText(comp: JTextComponent): String {
            val doc = comp.document

            val dot = comp.caretPosition
            val root = doc.defaultRootElement
            val index = root.getElementIndex(dot)
            val elem = root.getElement(index)
            var start = elem.startOffset
            var len = dot - start
            try {
                doc.getText(start, len)
            } catch (ble: BadLocationException) {
                ble.printStackTrace()
                return EMPTY_STRING
            }

            val segEnd = seg.offset + len
            start = segEnd - 1
            while (start >= seg.offset && seg.array != null && isValidChar(seg.array[start])) {
                start--
            }
            start++

            len = segEnd - start
            return if (len == 0) EMPTY_STRING else String(seg.array, start, len)
        }

    }
    return cp
}

internal class NoInsetTabbedPaneUI : MetalTabbedPaneUI() {

    override fun installDefaults() {
        super.installDefaults()
        this.tabAreaBackground = Color.RED
        this.selectColor = BACKGROUND_LIGHTER
        this.selectHighlight = BACKGROUND_LIGHTER
        this.highlight = BACKGROUND_LIGHTER
    }

    fun overrideContentBorderInsetsOfUI() {
        if (this.contentBorderInsets != null) {
            this.contentBorderInsets.top = 0
            this.contentBorderInsets.left = 0
            this.contentBorderInsets.right = 0
            this.contentBorderInsets.bottom = 2
        }
    }
}

class MyTabbedPane : JTabbedPane() {

    init {
        this.tabPlacement = SwingConstants.TOP
        val ui = NoInsetTabbedPaneUI()
        this.setUI(ui)
        ui.overrideContentBorderInsetsOfUI()
    }

}

private fun addTab(tabbedPane: MyTabbedPane, title: String, font: Font, initialContenxt: String = "",
                   languageSupport: LanguageSupport = noneLanguageSupport,
                   file: File? = null) {
    val panel = makeTextPanel(font, languageSupport, initialContenxt, file)
    tabbedPane.addTab(title, null, panel, "Go to $title")
    tabbedPane.setForegroundAt(tabbedPane.tabCount - 1, Color.white)
    tabbedPane.setBackgroundAt(tabbedPane.tabCount - 1, BACKGROUND_DARKER)
}

val APP_TITLE = "Kanvas"

val font: Font = Font.createFont(Font.TRUETYPE_FONT, Object().javaClass.getResourceAsStream("/CutiveMono-Regular.ttf"))
        .deriveFont(24.0f)

//
// Commands
//

private fun saveAsCommand(tabbedPane : MyTabbedPane) {
    if (tabbedPane.selectedComponent == null) {
        return
    }
    val fc = JFileChooser()
    val res = fc.showOpenDialog(tabbedPane)
    if (res == JFileChooser.APPROVE_OPTION) {
        (tabbedPane.selectedComponent as TextPanel).file = fc.selectedFile
        val languageSupport = languageSupportRegistry.languageSupportForFile(fc.selectedFile)
        fc.selectedFile.writeText((tabbedPane.selectedComponent as TextPanel).text)
        (tabbedPane.selectedComponent as TextPanel).title = fc.selectedFile.name
        (tabbedPane.selectedComponent as TextPanel).changeLanguageSupport(languageSupport)
    }
}

private fun saveCommand(tabbedPane : MyTabbedPane) {
    if (tabbedPane.selectedComponent == null) {
        return
    }
    val file = (tabbedPane.selectedComponent as TextPanel).file
    if (file == null) {
        saveAsCommand(tabbedPane)
    } else {
        file.writeText((tabbedPane.selectedComponent as TextPanel).text)
    }
}

private fun closeCommand(tabbedPane : MyTabbedPane) {
    if (tabbedPane.selectedComponent == null) {
        return
    }
    (tabbedPane.selectedComponent as TextPanel).close()
}

private fun openCommand(tabbedPane: MyTabbedPane) {
    val fc = JFileChooser()
    val res = fc.showOpenDialog(tabbedPane)
    if (res == JFileChooser.APPROVE_OPTION) {
        addTab(tabbedPane, fc.selectedFile.name, font, fc.selectedFile.readText(Charset.defaultCharset()),
                languageSupportRegistry.languageSupportForFile(fc.selectedFile),
                fc.selectedFile)
    }
}

//
// Public API
//

fun createAndShowKanvasGUI() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val xToolkit = Toolkit.getDefaultToolkit()
    val awtAppClassNameField = xToolkit.javaClass.getDeclaredField("awtAppClassName")
    awtAppClassNameField.isAccessible = true
    awtAppClassNameField.set(xToolkit, APP_TITLE)
    
    val frame = JFrame(APP_TITLE)
    frame.background = BACKGROUND_DARKER
    frame.contentPane.background = BACKGROUND_DARKER
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val tabbedPane = MyTabbedPane()
    frame.contentPane.add(tabbedPane)

    val menuBar = JMenuBar()
    val fileMenu = JMenu("File")
    menuBar.add(fileMenu)
    val open = JMenuItem("Open")
    open.addActionListener { openCommand(tabbedPane) }
    fileMenu.add(open)
    val new = JMenuItem("New")
    new.addActionListener { addTab(tabbedPane, "<UNNAMED>", font) }
    fileMenu.add(new)
    val save = JMenuItem("Save")
    save.addActionListener { saveCommand(tabbedPane) }
    fileMenu.add(save)
    val saveAs = JMenuItem("Save as")
    saveAs.addActionListener { saveAsCommand(tabbedPane) }
    fileMenu.add(saveAs)
    val close = JMenuItem("Close")
    close.addActionListener { closeCommand(tabbedPane) }
    fileMenu.add(close)
    frame.jMenuBar = menuBar

    frame.pack()
    if (frame.width < 500) {
        frame.size = Dimension(500, 500)
    }
    frame.isVisible = true
}

fun main(args: Array<String>) {
    languageSupportRegistry.register("py", pythonLanguageSupport)
    languageSupportRegistry.register("sandy", sandyLanguageSupport)
    SwingUtilities.invokeLater { createAndShowKanvasGUI() }
}