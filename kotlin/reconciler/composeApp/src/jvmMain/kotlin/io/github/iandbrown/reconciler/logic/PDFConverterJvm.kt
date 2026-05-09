package io.github.iandbrown.reconciler.logic

import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.IOException
import kotlin.collections.iterator

internal class PDFConverterJVM : AbstractPDFConverter {
    private val items: Map<RectArea, String>

    constructor(source: ByteArray) {
        val document = Loader.loadPDF(source)
        val textStripper = MyTextStripper()
        val logger = LoggerFactory.get(ImportDefinitionViewModel::class.simpleName!!)

        textStripper.startPage = 0
        textStripper.endPage = document.numberOfPages - 1
        textStripper.getText(document)
        items = textStripper.areaHolder.items

        for (e in items) {
            logger.debug {"Pair(RectArea(${e.key.left}F, ${e.key.right}F, ${e.key.top}F, ${e.key.bottom}F), \"${e.value}\"),"}
        }

        document.close()
    }

    override fun getItems(): Map<RectArea, String> = items
}

private class MyTextStripper : PDFTextStripper() {
    val areaHolder = TextAreaHolder()

    @Throws(IOException::class)
    override fun startPage(page: PDPage?) {
        val cropBox = page?.cropBox!!
        areaHolder.startPage(cropBox.lowerLeftX, cropBox.lowerLeftY, cropBox.height)
        super.startPage(page)
    }

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        areaHolder.saveCurrent()
        super.writeLineSeparator()
    }

    @Throws(IOException::class)
    override fun getText(doc: PDDocument?): String? {
        sortByPosition = true
        areaHolder.clear(dropThreshold)
        return super.getText(doc)
    }

    @Throws(IOException::class)
    override fun writeWordSeparator() {
        areaHolder.saveCurrent()

        super.writeWordSeparator()
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition?>?) {
        areaHolder.stringAt(text,
            textPositions?.asSequence()?.map { RectArea(it?.x!!, it.x + it.width, it.y, it.y + it.height) }!!
        )
        super.writeString(text, textPositions)
    }
}
