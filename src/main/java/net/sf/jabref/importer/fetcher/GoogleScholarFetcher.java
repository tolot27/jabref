/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fetcher;

import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibtexEntry;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleScholarFetcher implements PreviewEntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(GoogleScholarFetcher.class);

    private boolean hasRunConfig;
    private static final int MAX_ENTRIES_TO_LOAD = 50;
    private static final String QUERY_MARKER = "___QUERY___";
    private static final String URL_START = "http://scholar.google.com";
    private static final String URL_SETTING = "http://scholar.google.com/scholar_settings";
    private static final String URL_SETPREFS = "http://scholar.google.com/scholar_setprefs";
    private static final String SEARCH_URL = GoogleScholarFetcher.URL_START + "/scholar?q=" + GoogleScholarFetcher.QUERY_MARKER
            + "&amp;hl=en&amp;btnG=Search";

    private static final Pattern BIBTEX_LINK_PATTERN = Pattern.compile("<a href=\"([^\"]*)\"[^>]*>[A-Za-z ]*BibTeX");
    private static final Pattern TITLE_START_PATTERN = Pattern.compile("<div class=\"gs_ri\">");
    private static final Pattern LINK_PATTERN = Pattern.compile("<h3 class=\"gs_rt\"><a href=\"([^\"]*)\">");
    private static final Pattern TITLE_END_PATTERN = Pattern.compile("<div class=\"gs_fl\">");

    private final HashMap<String, String> entryLinks = new HashMap<>();
    //final static Pattern NEXT_PAGE_PATTERN = Pattern.compile(
    //        "<a href=\"([^\"]*)\"><span class=\"SPRITE_nav_next\"> </span><br><span style=\".*\">Next</span></a>");

    private boolean stopFetching;


    @Override
    public int getWarningLimit() {
        return 10;
    }

    @Override
    public int getPreferredPreviewHeight() {
        return 100;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        return false;
    }

    @Override
    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status) {
        entryLinks.clear();
        stopFetching = false;
        try {
            if (!hasRunConfig) {
                runConfig();
                hasRunConfig = true;
            }
            Map<String, JLabel> citations = getCitations(query);
            for (Map.Entry<String, JLabel> linkEntry : citations.entrySet()) {
                preview.addEntry(linkEntry.getKey(), linkEntry.getValue());
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(Localization.lang("Error fetching from Google Scholar"));
            return false;
        }
    }

    @Override
    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector) {
        int toDownload = 0;
        int downloaded = 0;
        for (Map.Entry<String, Boolean> selEntry : selection.entrySet()) {
            boolean isSelected = selEntry.getValue();
            if (isSelected) {
                toDownload++;
            }
        }
        if (toDownload == 0) {
            return;
        }

        for (Map.Entry<String, Boolean> selEntry : selection.entrySet()) {
            if (stopFetching) {
                break;
            }
            inspector.setProgress(downloaded, toDownload);
            boolean isSelected = selEntry.getValue();
            if (isSelected) {
                downloaded++;
                try {
                    BibtexEntry entry = downloadEntry(selEntry.getKey());
                    inspector.addEntry(entry);
                } catch (IOException e) {
                    LOGGER.warn("Cannot download entry from Google scholar", e);
                }
            }
        }

    }

    @Override
    public String getTitle() {
        return "Google Scholar";
    }

    @Override
    public String getHelpPage() {
        return "GoogleScholarHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }

    /*  Used for debugging */
    /*    private static void save(String filename, String content) throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
            out.write(content);
        }
    }
    */

    private static void runConfig() throws IOException {
        try {
            new URLDownload(new URL("http://scholar.google.com")).downloadToString();
            //save("setting.html", ud.getStringContent());
            String settingsPage = new URLDownload(new URL(GoogleScholarFetcher.URL_SETTING)).downloadToString();
            // Get the form items and their values from the page:
            HashMap<String, String> formItems = GoogleScholarFetcher.getFormElements(settingsPage);
            // Override the important ones:
            formItems.put("scis", "yes");
            formItems.put("scisf", "4");
            formItems.put("num", String.valueOf(GoogleScholarFetcher.MAX_ENTRIES_TO_LOAD));
            StringBuilder ub = new StringBuilder(GoogleScholarFetcher.URL_SETPREFS + "?");
            for (Iterator<String> i = formItems.keySet().iterator(); i.hasNext();) {
                String name = i.next();
                ub.append(name).append("=").append(formItems.get(name));
                if (i.hasNext()) {
                    ub.append("&");
                }
            }
            ub.append("&submit=");
            // Download the URL to set preferences:
            new URLDownload(new URL(ub.toString())).downloadToString();

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Unsupported encoding.", ex);
        }
    }

    /**
     * @param query The search term to query Google Scholar for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private Map<String, JLabel> getCitations(String query) throws IOException {
        String urlQuery;
        LinkedHashMap<String, JLabel> res = new LinkedHashMap<>();
        try {
            urlQuery = GoogleScholarFetcher.SEARCH_URL.replace(GoogleScholarFetcher.QUERY_MARKER,
                    URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
            int count = 1;
            String nextPage;
            while (((nextPage = getCitationsFromUrl(urlQuery, res)) != null)
                    && (count < 2)) {
                urlQuery = nextPage;
                count++;
                if (stopFetching) {
                    break;
                }
            }
            return res;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCitationsFromUrl(String urlQuery, Map<String, JLabel> ids) throws IOException {
        URL url = new URL(urlQuery);
        String cont = new URLDownload(url).downloadToString();
        //save("query.html", cont);
        Matcher m = GoogleScholarFetcher.BIBTEX_LINK_PATTERN.matcher(cont);
        int lastRegionStart = 0;
        while (m.find()) {
            String link = m.group(1).replaceAll("&amp;", "&");
            String pText;
            //System.out.println("regionStart: "+m.start());
            String part = cont.substring(lastRegionStart, m.start());
            Matcher titleS = GoogleScholarFetcher.TITLE_START_PATTERN.matcher(part);
            Matcher titleE = GoogleScholarFetcher.TITLE_END_PATTERN.matcher(part);
            boolean fS = titleS.find();
            boolean fE = titleE.find();
            //System.out.println("fs = "+fS+", fE = "+fE);
            //System.out.println(titleS.end()+" : "+titleE.start());
            if (fS && fE) {
                if (titleS.end() < titleE.start()) {
                    pText = part.substring(titleS.end(), titleE.start());
                } else {
                    pText = part;
                }
            } else {
                pText = link;
            }

            pText = pText.replaceAll("\\[PDF\\]", "");
            JLabel preview = new JLabel("<html>" + pText + "</html>");
            ids.put(link, preview);

            // See if we can extract the link Google Scholar puts on the entry's title.
            // That will be set as "url" for the entry if downloaded:
            Matcher linkMatcher = GoogleScholarFetcher.LINK_PATTERN.matcher(pText);
            if (linkMatcher.find()) {
                entryLinks.put(link, linkMatcher.group(1));
            }

            lastRegionStart = m.end();
        }

        /*m = NEXT_PAGE_PATTERN.matcher(cont);
        if (m.find()) {
            System.out.println("NEXT: "+URL_START+m.group(1).replaceAll("&amp;", "&"));
            return URL_START+m.group(1).replaceAll("&amp;", "&");
        }
        else*/
        return null;
    }

    private BibtexEntry downloadEntry(String link) throws IOException {
        try {
            URL url = new URL(GoogleScholarFetcher.URL_START + link);
            String s = new URLDownload(url).downloadToString();
            BibtexParser bp = new BibtexParser(new StringReader(s));
            ParserResult pr = bp.parse();
            if ((pr != null) && (pr.getDatabase() != null)) {
                Collection<BibtexEntry> entries = pr.getDatabase().getEntries();
                if (entries.size() == 1) {
                    BibtexEntry entry = entries.iterator().next();
                    boolean clearKeys = true;
                    if (clearKeys) {
                        entry.setField(BibtexEntry.KEY_FIELD, null);
                    }
                    // If the entry's url field is not set, and we have stored an url for this
                    // entry, set it:
                    if (entry.getField("url") == null) {
                        String storedUrl = entryLinks.get(link);
                        if (storedUrl != null) {
                            entry.setField("url", storedUrl);
                        }
                    }

                    // Clean up some remaining HTML code from Elsevier(?) papers
                    // Search for: Poincare algebra
                    // to see an example
                    String title = entry.getField("title");
                    if (title != null) {
                        String newtitle = title.replaceAll("<.?i>([^<]*)</i>", "$1");
                        if (!newtitle.equals(title)) {
                            entry.setField("title", newtitle);
                        }
                    }

                    return entry;
                } else if (entries.isEmpty()) {
                    LOGGER.warn("No entry found! (" + link + ")");
                    return null;
                } else {
                    LOGGER.debug(entries.size() + " entries found! (" + link + ")");
                    return null;
                }
            }
            LOGGER.warn("Parser failed! (" + link + ")");
            return null;
        } catch (MalformedURLException ex) {
            LOGGER.error("Malformed URL.", ex);
            return null;
        }
    }


    private static final Pattern inputPattern = Pattern.compile("<input type=([^ ]+) name=([^ ]+) value=([^> ]+)");


    private static HashMap<String, String> getFormElements(String page) {
        Matcher m = GoogleScholarFetcher.inputPattern.matcher(page);
        HashMap<String, String> items = new HashMap<>();
        while (m.find()) {
            String name = m.group(2);
            if ((name.length() > 2) && (name.charAt(0) == '"')
                    && (name.charAt(name.length() - 1) == '"')) {
                name = name.substring(1, name.length() - 1);
            }
            String value = m.group(3);
            if ((value.length() > 2) && (value.charAt(0) == '"')
                    && (value.charAt(value.length() - 1) == '"')) {
                value = value.substring(1, value.length() - 1);
            }
            items.put(name, value);
        }
        return items;
    }
}
