package flexmark.demo;

import flexmark.demo.FlexmarkDocGenerator.Type;
import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Flexmark DocGeneration
 *
 * @author delker
 */
public class FlexmarkDocGenerationTest {

  final static String TEST_FILE = "src/test/resources/test.md";
  final static String OUT_FILEBASE = "target/test_out";
  
  private static final Logger LOG = Logger.getLogger(FlexmarkDocGenerationTest.class.getName());

  public FlexmarkDocGenerationTest() {
  }

  /**
   * Test of generateDocument method, of class FlexmarkDocGenerator.
   */
  @org.junit.jupiter.api.Test
  public void testGenerateDocument() throws Exception {
    File outFile;
    FlexmarkDocGenerator generator = new FlexmarkDocGenerator();
    
    // generate HTML
    LOG.info("Generating HTML...");
    generator.generateDocument(TEST_FILE, OUT_FILEBASE, FlexmarkDocGenerator.Type.HTML);
    outFile = getOutFile(OUT_FILEBASE, Type.HTML);
    assertTrue(Files.exists(outFile.toPath()), "expected file " + outFile + " does not exist");
    assertTrue(Files.size(outFile.toPath())>0, "emtpy file " + outFile);
    
    // generate DOCX
    LOG.info("Generating DOCX...");
    generator.generateDocument(TEST_FILE, OUT_FILEBASE, FlexmarkDocGenerator.Type.DOCX);
    outFile = getOutFile(OUT_FILEBASE, Type.DOCX);
    assertTrue(Files.exists(outFile.toPath()), "expected file " + outFile + " does not exist");
    assertTrue(Files.size(outFile.toPath())>0, "emtpy file " + outFile);
    
    // generate PDF
    LOG.info("Generating PDF...");
    generator.generateDocument(TEST_FILE, OUT_FILEBASE, FlexmarkDocGenerator.Type.PDF);
    outFile = getOutFile(OUT_FILEBASE, Type.PDF);
    assertTrue(Files.exists(outFile.toPath()), "expected file " + outFile + " does not exist");
    assertTrue(Files.size(outFile.toPath())>0, "emtpy file " + outFile);
    
  }

  private File getOutFile(String filebase, Type type) {
    return new File(
            String.format("%s.%s", filebase, type.name().toLowerCase())
    );
  }
}
