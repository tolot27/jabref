package net.sf.jabref.importer;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.database.BibtexDatabase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class OpenDatabaseActionTest {

    private final Charset defaultEncoding = StandardCharsets.UTF_8;
    private final File bibNoHeader = new File(OpenDatabaseActionTest.class.getResource("headerless.bib").getFile());
    private final File bibWrongHeader = new File(
            OpenDatabaseActionTest.class.getResource("wrong-header.bib").getFile());
    private final File bibHeader = new File(OpenDatabaseActionTest.class.getResource("encoding-header.bib").getFile());
    private final File bibHeaderAndSignature = new File(
            OpenDatabaseActionTest.class.getResource("jabref-header.bib").getFile());

    @BeforeClass
    public static void setUpGlobalsPrefs() {
        // otherwise FieldContentParser (called by BibtexParser) crashes
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void useFallbackEncodingIfNoHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibNoHeader, defaultEncoding);
        Assert.assertEquals(defaultEncoding, result.getEncoding());
    }

    @Test
    public void useFallbackEncodingIfUnknownHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibWrongHeader, defaultEncoding);
        Assert.assertEquals(defaultEncoding, result.getEncoding());
    }

    @Test
    public void useSpecifiedEncoding() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeader, StandardCharsets.US_ASCII);
        Assert.assertEquals(StandardCharsets.UTF_8, result.getEncoding());
    }

    @Test
    public void useSpecifiedEncodingWithSignature() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeaderAndSignature, StandardCharsets.US_ASCII);
        Assert.assertEquals(StandardCharsets.UTF_8, result.getEncoding());
    }

    @Test
    public void entriesAreParsedNoHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibNoHeader, defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void entriesAreParsedHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeader, defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void entriesAreParsedHeaderAndSignature() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeaderAndSignature, defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void noVersionForNoHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibNoHeader, defaultEncoding);

        // Version
        Assert.assertEquals(0, result.getJabrefMajorVersion());
        Assert.assertEquals(0, result.getJabrefMinorVersion());
    }

    @Test
    public void noVersionForWrongHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibWrongHeader, defaultEncoding);

        // Version
        Assert.assertEquals(0, result.getJabrefMajorVersion());
        Assert.assertEquals(0, result.getJabrefMinorVersion());
    }

    @Test
    public void noVersionForHeaderWithoutSignature() throws IOException {
        // newer JabRef versions do not put a header
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeader, defaultEncoding);

        // Version
        Assert.assertEquals(0, result.getJabrefMajorVersion());
        Assert.assertEquals(0, result.getJabrefMinorVersion());
    }

    @Test
    public void versionFromSignature() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeaderAndSignature, defaultEncoding);

        // Version
        Assert.assertEquals(2, result.getJabrefMajorVersion());
        Assert.assertEquals(9, result.getJabrefMinorVersion());
    }
}