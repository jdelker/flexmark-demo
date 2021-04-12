package flexmark.demo;

import com.vladsch.flexmark.docx.converter.DocxRenderer;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 *
 * @author delker
 */
public class FlexmarkDocGenerator {

  public enum Type {
    HTML, PDF, DOCX
  };

  public void generateDocument(String inFileName, String targetPath, Type type) throws IOException, Docx4JException {
    File inFile = new File(inFileName);
    if (!inFile.canRead()) {
      throw new FileNotFoundException(inFileName);
    }

    File outFile = getOutFile(targetPath, type);

    // parse input file
    Node document = parseInput(inFile);

    switch (type) {
      case HTML:
        writeHtml(document, outFile);
        break;

      case DOCX:
        writeDocx(document, outFile);
        break;

      case PDF:
        writePdf(document, outFile);
        break;
    }
  }

  private Node parseInput(File file) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));

    MutableDataSet options = new MutableDataSet();
    options.setFrom(ParserEmulationProfile.MULTI_MARKDOWN);
    options.set(Parser.EXTENSIONS, Arrays.asList(
            AbbreviationExtension.create(),
            DefinitionExtension.create(),
            FootnoteExtension.create(),
            TablesExtension.create(),
            TypographicExtension.create()
    ));

    Parser parser = Parser.builder(options).build();
    return parser.parseReader(reader);
  }

  private void writeHtml(Node document, File outFile) throws IOException {
    try ( BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
      MutableDataSet options = new MutableDataSet();

      HtmlRenderer renderer = HtmlRenderer.builder(options).build();
      renderer.render(document, writer);
    }
  }

  private void writeDocx(Node document, File outFile) throws IOException, Docx4JException {

    // see https://github.com/vsch/flexmark-java/wiki/Docx-Renderer-Extension
    //
    BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

    MutableDataSet options = new MutableDataSet();
    options.set(DocxRenderer.SUPPRESS_HTML, true)
            // the following two are needed to allow doc relative and site relative address resolution
            .set(DocxRenderer.DOC_RELATIVE_URL, "file:///Users/vlad/src/pdf") // this will be used for URLs like 'images/...' or './' or '../'
            .set(DocxRenderer.DOC_ROOT_URL, "file:///Users/vlad/src/pdf"); // this will be used for URLs like: '/...'

    DocxRenderer renderer = DocxRenderer.builder(options).build();

    WordprocessingMLPackage template = DocxRenderer.getDefaultTemplate();
    renderer.render(document, template);

    template.save(outFile, Docx4J.FLAG_SAVE_ZIP_FILE);
  }

  private void writePdf(Node document, File outFile) throws IOException {

    // see https://github.com/vsch/flexmark-java/wiki/PDF-Renderer-Converter
    //
    MutableDataSet options = new MutableDataSet();

    HtmlRenderer renderer = HtmlRenderer.builder(options).build();
    String html = renderer.render(document);

    html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n"
            + ""
            // add your stylesheets, scripts styles etc.
            // uncomment line below for adding style for custom embedded fonts
            // nonLatinFonts +
            + "</head><body>" + html + "\n"
            + "</body></html>";

//    DataHolder PDFOPTIONS = PegdownOptionsAdapter.flexmarkOptions(
//            Extensions.ALL & ~(Extensions.ANCHORLINKS | Extensions.EXTANCHORLINKS_WRAP),
//            TocExtension.create()).toMutable()
//            .set(TocExtension.LIST_CLASS, PdfConverterExtension.DEFAULT_TOC_LIST_CLASS)
//            .toImmutable();
    PdfConverterExtension.exportToPdf(outFile.toString(), html, "", options);
  }

  private File getOutFile(String filebase, Type type) {
    return new File(
            String.format("%s.%s", filebase, type.name().toLowerCase())
    );
  }

}
